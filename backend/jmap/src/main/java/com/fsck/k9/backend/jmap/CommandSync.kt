package com.fsck.k9.backend.jmap

import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.logging.Timber
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.internet.MimeMessage
import java.util.Date
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import rs.ltt.jmap.client.JmapClient
import rs.ltt.jmap.client.api.MethodErrorResponseException
import rs.ltt.jmap.client.api.UnauthorizedException
import rs.ltt.jmap.client.http.HttpAuthentication
import rs.ltt.jmap.client.session.Session
import rs.ltt.jmap.common.entity.Email
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition
import rs.ltt.jmap.common.entity.query.EmailQuery
import rs.ltt.jmap.common.method.call.email.GetEmailMethodCall
import rs.ltt.jmap.common.method.call.email.QueryChangesEmailMethodCall
import rs.ltt.jmap.common.method.call.email.QueryEmailMethodCall
import rs.ltt.jmap.common.method.response.email.GetEmailMethodResponse
import rs.ltt.jmap.common.method.response.email.QueryChangesEmailMethodResponse
import rs.ltt.jmap.common.method.response.email.QueryEmailMethodResponse

class CommandSync(
    private val backendStorage: BackendStorage,
    private val jmapClient: JmapClient,
    private val okHttpClient: OkHttpClient,
    private val accountId: String,
    private val httpAuthentication: HttpAuthentication,
) {

    fun sync(folderServerId: String, syncConfig: SyncConfig, listener: SyncListener) {
        try {
            val backendFolder = backendStorage.getFolder(folderServerId)
            listener.syncStarted(folderServerId)

            val limit = if (backendFolder.visibleLimit > 0) backendFolder.visibleLimit.toLong() else null

            val queryState = backendFolder.getFolderExtraString(EXTRA_QUERY_STATE)
            if (queryState == null) {
                fullSync(backendFolder, folderServerId, syncConfig, limit, listener)
            } else {
                deltaSync(backendFolder, folderServerId, syncConfig, limit, queryState, listener)
            }

            listener.syncFinished(folderServerId)
        } catch (e: UnauthorizedException) {
            Timber.e(e, "Authentication failure during sync")

            val exception = AuthenticationFailedException(e.message ?: "Authentication failed", e)
            listener.syncFailed(folderServerId, "Authentication failed", exception)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected failure during sync")

            listener.syncFailed(folderServerId, "Unexpected failure", e)
        }
    }

    private fun fullSync(
        backendFolder: BackendFolder,
        folderServerId: String,
        syncConfig: SyncConfig,
        limit: Long?,
        listener: SyncListener,
    ) {
        val cachedServerIds: Set<String> = backendFolder.getMessageServerIds()

        if (limit != null) {
            Timber.d("Fetching %d latest messages in %s (%s)", limit, backendFolder.name, folderServerId)
        } else {
            Timber.d("Fetching all messages in %s (%s)", backendFolder.name, folderServerId)
        }

        val queryEmailCall = jmapClient.call(
            QueryEmailMethodCall.builder()
                .accountId(accountId)
                .query(createEmailQuery(folderServerId))
                .limit(limit)
                .build(),
        )
        val queryEmailResponse = queryEmailCall.getMainResponseBlocking<QueryEmailMethodResponse>()
        val queryState = if (queryEmailResponse.isCanCalculateChanges) queryEmailResponse.queryState else null
        val remoteServerIds = queryEmailResponse.ids.toSet()

        val destroyServerIds = (cachedServerIds - remoteServerIds).toList()
        val newServerIds = remoteServerIds - cachedServerIds

        handleFolderUpdates(backendFolder, folderServerId, destroyServerIds, newServerIds, queryState, listener)

        val refreshServerIds = cachedServerIds.intersect(remoteServerIds)
        refreshMessageFlags(backendFolder, syncConfig, refreshServerIds)
    }

    private fun createEmailQuery(folderServerId: String): EmailQuery? {
        val filter = EmailFilterCondition.builder()
            .inMailbox(folderServerId)
            .build()

        // FIXME: Add sort parameter
        return EmailQuery.of(filter)
    }

    private fun deltaSync(
        backendFolder: BackendFolder,
        folderServerId: String,
        syncConfig: SyncConfig,
        limit: Long?,
        queryState: String,
        listener: SyncListener,
    ) {
        Timber.d("Updating messages in %s (%s)", backendFolder.name, folderServerId)

        val emailQuery = createEmailQuery(folderServerId)
        val queryChangesEmailCall = jmapClient.call(
            QueryChangesEmailMethodCall.builder()
                .accountId(accountId)
                .sinceQueryState(queryState)
                .query(emailQuery)
                .build(),
        )

        val queryChangesEmailResponse = try {
            queryChangesEmailCall.getMainResponseBlocking<QueryChangesEmailMethodResponse>()
        } catch (e: MethodErrorResponseException) {
            if (e.methodErrorResponse.type == ERROR_CANNOT_CALCULATE_CHANGES) {
                Timber.d("Server responded with '$ERROR_CANNOT_CALCULATE_CHANGES'; switching to full sync")

                backendFolder.saveQueryState(null)
                fullSync(backendFolder, folderServerId, syncConfig, limit, listener)
                return
            }

            throw e
        }

        val cachedServerIds = backendFolder.getMessageServerIds()

        val removedServerIds = queryChangesEmailResponse.removed.toSet()
        val addedServerIds = queryChangesEmailResponse.added.map { it.item }.toSet()
        val newQueryState = queryChangesEmailResponse.newQueryState

        // An email can appear in both the 'removed' and the 'added' properties, e.g. when its position in the list
        // changes. But we don't want to remove a message from the database only to download it again right away.
        val retainedServerIds = removedServerIds.intersect(addedServerIds)
        val destroyServerIds = (removedServerIds - retainedServerIds).toList()
        val newServerIds = addedServerIds - retainedServerIds

        handleFolderUpdates(backendFolder, folderServerId, destroyServerIds, newServerIds, newQueryState, listener)

        val refreshServerIds = cachedServerIds - destroyServerIds
        refreshMessageFlags(backendFolder, syncConfig, refreshServerIds)
    }

    private fun handleFolderUpdates(
        backendFolder: BackendFolder,
        folderServerId: String,
        destroyServerIds: List<String>,
        newServerIds: Set<String>,
        newQueryState: String?,
        listener: SyncListener,
    ) {
        if (destroyServerIds.isNotEmpty()) {
            Timber.d("Removing messages no longer on server: %s", destroyServerIds)
            backendFolder.destroyMessages(destroyServerIds)
        }

        if (newServerIds.isEmpty()) {
            Timber.d("No new messages on server")
            backendFolder.saveQueryState(newQueryState)
            return
        }

        Timber.d("New messages on server: %s", newServerIds)
        val session = jmapClient.session.get()
        val maxObjectsInGet = session.maxObjectsInGet
        val messageInfoList = fetchMessageInfo(session, maxObjectsInGet, newServerIds)

        val total = messageInfoList.size
        messageInfoList.forEachIndexed { index, messageInfo ->
            Timber.v("Downloading message %s (%s)", messageInfo.serverId, messageInfo.downloadUrl)
            val message = downloadMessage(messageInfo.downloadUrl)
            if (message != null) {
                message.apply {
                    uid = messageInfo.serverId
                    setInternalSentDate(messageInfo.receivedAt)
                    setFlags(messageInfo.flags, true)
                }

                backendFolder.saveMessage(message, MessageDownloadState.FULL)
            } else {
                Timber.d("Failed to download message: %s", messageInfo.serverId)
            }

            listener.syncProgress(folderServerId, index + 1, total)
        }

        backendFolder.saveQueryState(newQueryState)
    }

    private fun fetchMessageInfo(session: Session, maxObjectsInGet: Int, emailIds: Set<String>): List<MessageInfo> {
        return emailIds
            .chunked(maxObjectsInGet) { emailIdsChunk ->
                getEmailPropertiesFromServer(emailIdsChunk, INFO_PROPERTIES)
            }
            .flatten()
            .map { email ->
                email.toMessageInfo(session)
            }
    }

    private fun getEmailPropertiesFromServer(emailIdsChunk: List<String>, properties: Array<String>): List<Email> {
        val getEmailCall = jmapClient.call(
            GetEmailMethodCall.builder()
                .accountId(accountId)
                .ids(emailIdsChunk.toTypedArray())
                .properties(properties)
                .build(),
        )

        val getEmailResponse = getEmailCall.getMainResponseBlocking<GetEmailMethodResponse>()
        return getEmailResponse.list.toList()
    }

    private fun Email.toMessageInfo(session: Session): MessageInfo {
        val downloadUrl = session.getDownloadUrl(accountId, blobId, blobId, "application/octet-stream")
        return MessageInfo(id, downloadUrl, receivedAt, keywords.toFlags())
    }

    private fun downloadMessage(downloadUrl: HttpUrl): MimeMessage? {
        val request = Request.Builder()
            .url(downloadUrl)
            .apply {
                httpAuthentication.authenticate(this)
            }
            .build()

        return okHttpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val inputStream = response.body!!.byteStream()
                MimeMessage.parseMimeMessage(inputStream, false)
            } else {
                null
            }
        }
    }

    private fun refreshMessageFlags(backendFolder: BackendFolder, syncConfig: SyncConfig, emailIds: Set<String>) {
        if (emailIds.isEmpty()) return

        Timber.v("Fetching flags for messages: %s", emailIds)

        val session = jmapClient.session.get()
        val maxObjectsInGet = session.maxObjectsInGet

        emailIds
            .asSequence()
            .chunked(maxObjectsInGet) { emailIdsChunk ->
                getEmailPropertiesFromServer(emailIdsChunk, FLAG_PROPERTIES)
            }
            .flatten()
            .forEach { email ->
                syncFlagsForMessage(backendFolder, syncConfig, email)
            }
    }

    private fun syncFlagsForMessage(backendFolder: BackendFolder, syncConfig: SyncConfig, email: Email) {
        val messageServerId = email.id
        val localFlags = backendFolder.getMessageFlags(messageServerId)
        val remoteFlags = email.keywords.toFlags()
        for (flag in syncConfig.syncFlags) {
            val flagSetOnServer = flag in remoteFlags
            val flagSetLocally = flag in localFlags
            if (flagSetOnServer != flagSetLocally) {
                backendFolder.setMessageFlag(messageServerId, flag, flagSetOnServer)
            }
        }
    }

    private fun Map<String, Boolean>?.toFlags(): Set<Flag> {
        return if (this == null) {
            emptySet()
        } else {
            filterValues { it }.keys
                .mapNotNull { keyword -> keyword.toFlag() }
                .toSet()
        }
    }

    private fun String.toFlag(): Flag? = when (this) {
        "\$seen" -> Flag.SEEN
        "\$flagged" -> Flag.FLAGGED
        "\$draft" -> Flag.DRAFT
        "\$answered" -> Flag.ANSWERED
        "\$forwarded" -> Flag.FORWARDED
        else -> null
    }

    private fun BackendFolder.saveQueryState(queryState: String?) {
        setFolderExtraString(EXTRA_QUERY_STATE, queryState)
    }

    companion object {
        private const val EXTRA_QUERY_STATE = "jmapQueryState"
        private const val ERROR_CANNOT_CALCULATE_CHANGES = "cannotCalculateChanges"
        private val INFO_PROPERTIES = arrayOf("id", "blobId", "size", "receivedAt", "keywords")
        private val FLAG_PROPERTIES = arrayOf("id", "keywords")
    }
}

private data class MessageInfo(
    val serverId: String,
    val downloadUrl: HttpUrl,
    val receivedAt: Date,
    val flags: Set<Flag>,
)
