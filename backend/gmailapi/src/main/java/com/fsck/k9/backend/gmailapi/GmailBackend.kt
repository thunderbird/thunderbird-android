package com.fsck.k9.backend.gmailapi

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendPusher
import com.fsck.k9.backend.api.BackendPusherCallback
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.backend.api.updateFolders
import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Date
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.folder.api.FOLDER_DEFAULT_PATH_DELIMITER
import okhttp3.OkHttpClient

/** Native Gmail REST backend. It does not use IMAP/SMTP and has no push channel. */
@Suppress("TooManyFunctions")
class GmailBackend internal constructor(
    private val storage: BackendStorage,
    private val api: GmailApiClient,
) : Backend {
    constructor(storage: BackendStorage, tokens: OAuth2TokenProvider, logger: Logger) : this(
        storage,
        GmailApiClient(OkHttpClient(), tokens, logger),
    )

    override val supportsFlags = true
    override val supportsExpunge = true
    override val supportsMove = true
    override val supportsCopy = true
    override val supportsUpload = false
    override val savesSentMessagesToServer = true
    override val supportsTrashFolder = true
    override val supportsSearchByDate = false
    override val supportsFolderSubscriptions = false
    override val isPushCapable = false

    override fun refreshFolderList(): String {
        val labelsById = api.labels()
            .filter(::shouldMapGmailLabelToFolder)
            .associateBy { it.id }
            .toMutableMap()
        if (ALL_MAIL_LABEL_ID !in labelsById) {
            labelsById[ALL_MAIL_LABEL_ID] = GmailLabel(ALL_MAIL_LABEL_ID, "All Mail", "system")
        }
        val remote = labelsById.values.toList()
        val remoteIds = remote.mapTo(mutableSetOf()) { it.id }
        val existingIds = storage.getFolderServerIds().toSet()

        storage.updateFolders {
            remote.filter { it.id in existingIds }.forEach { label ->
                changeFolder(label.id, label.name, label.id.toFolderType())
            }
            createFolders(
                remote.filterNot { it.id in existingIds }
                    .map { FolderInfo(it.id, it.name, it.id.toFolderType(), FOLDER_DEFAULT_PATH_DELIMITER) },
            )
            deleteFolders((existingIds - remoteIds).toList())
        }
        return FOLDER_DEFAULT_PATH_DELIMITER
    }

    override fun sync(folderServerId: String, config: SyncConfig, listener: SyncListener) {
        listener.syncStarted(folderServerId)
        try {
            val folder = storage.getFolder(folderServerId)
            listener.syncHeadersStarted(folderServerId)

            val requestedLimit = (folder.visibleLimit.takeIf { it > 0 } ?: config.defaultVisibleLimit).coerceAtLeast(1)
            val previousLimit = folder.getFolderExtraNumber(EXTRA_VISIBLE_LIMIT)?.toInt()
            val fetchMore = config.fetchMoreMessagesRequested && previousLimit != null && requestedLimit > previousLimit

            val initialBaselineTimeMillis = if (previousLimit == null) System.currentTimeMillis() else null
            val initialHistoryId = if (previousLimit == null) api.currentHistoryId() else null
            val historyChanges = if (previousLimit != null && !fetchMore) readHistoryChanges(folder) else null
            val newMessageCount = when {
                previousLimit == null -> initialSync(
                    folder,
                    folderServerId,
                    requestedLimit,
                    config,
                    listener,
                    initialBaselineTimeMillis ?: System.currentTimeMillis(),
                )
                fetchMore -> fetchMore(folder, folderServerId, requestedLimit, config, listener)
                else -> refresh(folder, folderServerId, config, listener, requireNotNull(historyChanges))
            }

            if (previousLimit == null) {
                initialHistoryId?.let { folder.setFolderExtraString(EXTRA_HISTORY_ID, it) }
            } else if (!fetchMore) {
                historyChanges?.latestHistoryId?.let { folder.setFolderExtraString(EXTRA_HISTORY_ID, it) }
            }
            folder.setLastChecked(System.currentTimeMillis())
            listener.syncHeadersFinished(folderServerId, folder.getMessageServerIds().size, newMessageCount)
            listener.syncAuthenticationSuccess()
            listener.syncFinished(folderServerId)
        } catch (error: Exception) {
            listener.syncFailed(folderServerId, "Gmail sync failed", error)
        }
    }

    override fun downloadMessage(syncConfig: SyncConfig, folderServerId: String, messageServerId: String) {
        val folder = storage.getFolder(folderServerId)
        val partialKey = partialMessageKey(messageServerId)
        if (!folder.isMessagePresent(messageServerId) || folder.getFolderExtraString(partialKey) != null) {
            save(folder, messageServerId, syncConfig, forceFull = true)
        }
    }

    override fun downloadMessageStructure(folderServerId: String, messageServerId: String) = Unit

    override suspend fun downloadCompleteMessage(folderServerId: String, messageServerId: String) {
        // Full raw MIME, including attachment bytes, is downloaded only after explicit EML export.
        val rawMessage = api.rawMessage(messageServerId)
        val parsed = MimeMessage.parseMimeMessage(ByteArrayInputStream(rawMessage), false).apply {
            uid = messageServerId
        }
        storage.getFolder(folderServerId).saveMessage(parsed, MessageDownloadState.FULL)
        storage.getFolder(folderServerId).setFolderExtraString(partialMessageKey(messageServerId), null)
    }

    override fun setFlag(folderServerId: String, ids: List<String>, flag: Flag, newState: Boolean) {
        ids.forEach { id ->
            when (flag) {
                Flag.SEEN -> api.modify(id, remove = if (newState) listOf("UNREAD") else emptyList(), add = if (newState) emptyList() else listOf("UNREAD"))
                Flag.FLAGGED -> api.modify(id, add = if (newState) listOf("STARRED") else emptyList(), remove = if (newState) emptyList() else listOf("STARRED"))
                else -> throw UnsupportedOperationException("Gmail does not support changing $flag")
            }
        }
    }

    override fun markAllAsRead(folderServerId: String) {
        listIds(folderServerId, Int.MAX_VALUE).forEach { id -> api.modify(id, remove = listOf("UNREAD")) }
    }

    override fun expunge(folderServerId: String) {
        listIds(TRASH_LABEL_ID, Int.MAX_VALUE).forEach(api::deleteForever)
    }

    override fun deleteMessages(folderServerId: String, messageServerIds: List<String>) {
        if (folderServerId == TRASH_LABEL_ID) messageServerIds.forEach(api::deleteForever)
        else messageServerIds.forEach(api::trash)
    }

    override fun deleteAllMessages(folderServerId: String) {
        val messageIds = listIds(folderServerId, Int.MAX_VALUE)
        if (folderServerId == TRASH_LABEL_ID) messageIds.forEach(api::deleteForever)
        else messageIds.forEach(api::trash)
    }

    override fun moveMessages(source: String, target: String, ids: List<String>): Map<String, String> =
        ids.associateWith { id -> move(id, source, target) }

    override fun moveMessagesAndMarkAsRead(source: String, target: String, ids: List<String>): Map<String, String> =
        ids.associateWith { id -> move(id, source, target).also { api.modify(id, remove = listOf("UNREAD")) } }

    override fun copyMessages(source: String, target: String, ids: List<String>): Map<String, String> =
        ids.associateWith { id -> api.copy(id, target) }

    override fun search(
        folderServerId: String,
        query: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        performFullTextSearch: Boolean,
    ): List<String> {
        val gmailQuery = GmailSearchQuery.build(query, requiredFlags, forbiddenFlags, performFullTextSearch)
        val results = mutableListOf<String>()
        var pageToken: String? = null
        do {
            val page = api.listMessages(folderServerId, pageToken, maxResults = PAGE_SIZE, query = gmailQuery)
            results += page.messages.map { it.id }
            pageToken = page.nextPageToken
        } while (pageToken != null)
        return results.distinct()
    }

    override fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory) {
        val encodedId = part.serverExtra ?: part.getHeader(ATTACHMENT_ID_HEADER).firstOrNull()
            ?: throw MessagingException("Gmail part has no attachment reference")
        val attachmentId = decodeReference(encodedId)
            ?: throw MessagingException("Invalid Gmail attachment reference")
        val bytes = api.attachment(messageServerId, attachmentId)
        part.body = bodyFactory.createBody("binary", part.contentType, ByteArrayInputStream(bytes))
        part.setHeader("Content-Transfer-Encoding", "binary")
    }

    override fun findByMessageId(folderServerId: String, messageId: String): String? =
        api.findMessageIdByInternetMessageId(messageId)

    override fun uploadMessage(folderServerId: String, message: Message): String? =
        throw UnsupportedOperationException("Gmail upload is not supported")

    override fun sendMessage(message: Message) {
        api.send(message)
    }

    override fun createPusher(callback: BackendPusherCallback): BackendPusher = PollingPusher

    private fun initialSync(
        folder: BackendFolder,
        folderServerId: String,
        limit: Int,
        config: SyncConfig,
        listener: SyncListener,
        baselineTimeMillis: Long,
    ): Int {
        val (references, nextPageToken) = listWindow(folderServerId, null, limit)
        val newMessages = references.filterNot { folder.isMessagePresent(it.id) }
        saveReferences(folder, folderServerId, newMessages, config, listener, isOldMessage = false)
        folder.setFolderExtraString(EXTRA_OLDER_PAGE_TOKEN, nextPageToken)
        folder.setFolderExtraNumber(EXTRA_VISIBLE_LIMIT, limit.toLong())
        if (folder.getFolderExtraNumber(EXTRA_NEWEST_INTERNAL_DATE) == null) {
            folder.setFolderExtraNumber(EXTRA_NEWEST_INTERNAL_DATE, baselineTimeMillis)
        }
        folder.setMoreMessages(if (nextPageToken != null) BackendFolder.MoreMessages.TRUE else BackendFolder.MoreMessages.FALSE)
        return newMessages.size
    }

    private fun fetchMore(
        folder: BackendFolder,
        folderServerId: String,
        requestedLimit: Int,
        config: SyncConfig,
        listener: SyncListener,
    ): Int {
        val pageToken = folder.getFolderExtraString(EXTRA_OLDER_PAGE_TOKEN)
        if (pageToken == null) {
            folder.setMoreMessages(BackendFolder.MoreMessages.FALSE)
            folder.setFolderExtraNumber(EXTRA_VISIBLE_LIMIT, requestedLimit.toLong())
            return 0
        }

        val (references, nextPageToken) = listWindow(folderServerId, pageToken, FETCH_MORE_BATCH_SIZE)
        val newMessages = references.filterNot { folder.isMessagePresent(it.id) }
        saveReferences(folder, folderServerId, newMessages, config, listener, isOldMessage = true)
        folder.setFolderExtraString(EXTRA_OLDER_PAGE_TOKEN, nextPageToken)
        folder.setFolderExtraNumber(EXTRA_VISIBLE_LIMIT, requestedLimit.toLong())
        folder.setMoreMessages(if (nextPageToken != null) BackendFolder.MoreMessages.TRUE else BackendFolder.MoreMessages.FALSE)
        return newMessages.size
    }

    private fun refresh(
        folder: BackendFolder,
        folderServerId: String,
        config: SyncConfig,
        listener: SyncListener,
        historyChanges: GmailHistoryChanges,
    ): Int {
        if (historyChanges.requiresNewerSearch) {
            return refreshAfterHistoryReset(folder, folderServerId, config, listener)
        }

        if (config.syncRemoteDeletions) {
            val removed = historyChanges.deletedMessageIds.intersect(folder.getMessageServerIds())
            folder.destroyMessages(removed.toList())
            removed.forEach { listener.syncRemovedMessage(folderServerId, it) }
        }

        val newMessages = mutableListOf<Pair<String, GmailMessage>>()
        historyChanges.addedMessageIds.asSequence()
            .distinct()
            .filterNot { it in historyChanges.deletedMessageIds || folder.isMessagePresent(it) }
            .forEach { id ->
                val metadata = getMessageMetadataOrNull(id)
                if (metadata != null && metadata.belongsToFolder(folderServerId)) {
                    newMessages += id to metadata
                }
            }
        newMessages.forEachIndexed { index, (id, metadata) ->
            save(folder, id, config, metadataMessage = metadata)
            listener.syncNewMessage(folderServerId, id, isOldMessage = false)
            listener.syncProgress(folderServerId, index + 1, newMessages.size)
        }

        val newlySavedIds = newMessages.mapTo(mutableSetOf()) { it.first }
        historyChanges.changedMessageIds.asSequence()
            .filter { it !in newlySavedIds && it !in historyChanges.deletedMessageIds && folder.isMessagePresent(it) }
            .forEach { id ->
                val updated = getMessageMetadataOrNull(id)
                when {
                    updated == null && config.syncRemoteDeletions -> {
                        folder.destroyMessages(listOf(id))
                        listener.syncRemovedMessage(folderServerId, id)
                    }
                    updated != null && !updated.belongsToFolder(folderServerId) && config.syncRemoteDeletions -> {
                        folder.destroyMessages(listOf(id))
                        listener.syncRemovedMessage(folderServerId, id)
                    }
                    updated != null -> syncFlags(folder, folderServerId, id, updated.labelIds, config, listener)
                }
            }

        return newMessages.size
    }

    private fun readHistoryChanges(folder: BackendFolder): GmailHistoryChanges {
        val cursor = folder.getFolderExtraString(EXTRA_HISTORY_ID)
            ?: return GmailHistoryChanges.empty(api.currentHistoryId(), requiresNewerSearch = true)
        return try {
            api.history(cursor)
        } catch (error: GmailApiException) {
            // Recover from an expired cursor with a date-bounded query, never a full mailbox listing.
            if (error.statusCode == 404) {
                GmailHistoryChanges.empty(api.currentHistoryId(), requiresNewerSearch = true)
            } else {
                throw error
            }
        }
    }

    private fun refreshAfterHistoryReset(
        folder: BackendFolder,
        folderServerId: String,
        config: SyncConfig,
        listener: SyncListener,
    ): Int {
        val baselineTimeMillis = folder.getFolderExtraNumber(EXTRA_NEWEST_INTERNAL_DATE)
            ?: System.currentTimeMillis().also { folder.setFolderExtraNumber(EXTRA_NEWEST_INTERNAL_DATE, it) }
        val query = "after:${baselineTimeMillis / MILLIS_PER_SECOND}"
        val references = listWindow(folderServerId, null, Int.MAX_VALUE, query).first
        val newerMessages = references.asSequence()
            .distinctBy { it.id }
            .filterNot { folder.isMessagePresent(it.id) }
            .mapNotNull { reference ->
                getMessageMetadataOrNull(reference.id)
                    ?.takeIf { metadata ->
                        metadata.internalDateMillis?.let { it > baselineTimeMillis } == true &&
                            metadata.belongsToFolder(folderServerId)
                    }
                    ?.let { reference.id to it }
            }
            .toList()

        newerMessages.forEachIndexed { index, (id, metadata) ->
            save(folder, id, config, metadataMessage = metadata)
            listener.syncNewMessage(folderServerId, id, isOldMessage = false)
            listener.syncProgress(folderServerId, index + 1, newerMessages.size)
        }
        return newerMessages.size
    }

    private fun getMessageMetadataOrNull(id: String): GmailMessage? = try {
        api.getMessage(id, format = "metadata")
    } catch (error: GmailApiException) {
        if (error.statusCode == 404) null else throw error
    }

    private fun saveReferences(
        folder: BackendFolder,
        folderServerId: String,
        references: List<GmailRef>,
        config: SyncConfig,
        listener: SyncListener,
        isOldMessage: Boolean,
    ) {
        references.forEachIndexed { index, reference ->
            save(folder, reference.id, config)
            listener.syncNewMessage(folderServerId, reference.id, isOldMessage)
            listener.syncProgress(folderServerId, index + 1, references.size)
        }
    }

    private fun save(
        folder: BackendFolder,
        id: String,
        config: SyncConfig,
        forceFull: Boolean = false,
        metadataMessage: GmailMessage? = null,
    ) {
        val metadata = metadataMessage ?: api.getMessage(id, format = "metadata")
        val withinAutoDownloadLimit = metadata.sizeEstimate == null || metadata.sizeEstimate <= config.maximumAutoDownloadMessageSize.toLong()
        val isPartial = !forceFull && !withinAutoDownloadLimit
        val source = if (isPartial) metadata else api.getMessage(id, format = "full")
        val parsed = MimeMessage.parseMimeMessage(ByteArrayInputStream(api.createMime(source)), false).apply {
            uid = id
            setInternalSentDate(Date(source.internalDateMillis ?: metadata.internalDateMillis ?: System.currentTimeMillis()))
            setFlag(Flag.SEEN, "UNREAD" !in source.labelIds)
            setFlag(Flag.FLAGGED, "STARRED" in source.labelIds)
        }
        runBlocking {
            folder.saveMessage(parsed, if (isPartial) MessageDownloadState.PARTIAL else MessageDownloadState.FULL)
        }
        metadata.internalDateMillis?.let { receivedTime ->
            val currentNewestReceivedTime = folder.getFolderExtraNumber(EXTRA_NEWEST_INTERNAL_DATE)
            if (currentNewestReceivedTime == null || receivedTime > currentNewestReceivedTime) {
                folder.setFolderExtraNumber(EXTRA_NEWEST_INTERNAL_DATE, receivedTime)
            }
        }
        folder.setFolderExtraString(partialMessageKey(id), if (isPartial) "true" else null)
    }

    private fun syncFlags(
        folder: BackendFolder,
        folderServerId: String,
        messageId: String,
        labels: List<String>,
        config: SyncConfig,
        listener: SyncListener,
    ) {
        for (flag in config.syncFlags) {
            val remoteValue = when (flag) {
                Flag.SEEN -> "UNREAD" !in labels
                Flag.FLAGGED -> "STARRED" in labels
                else -> continue
            }
            if (folder.getMessageFlags(messageId).contains(flag) != remoteValue) {
                folder.setMessageFlag(messageId, flag, remoteValue)
                listener.syncFlagChanged(folderServerId, messageId)
            }
        }
    }

    private fun listIds(labelId: String, limit: Int): List<String> = buildList {
        var pageToken: String? = null
        do {
            val page = api.listMessages(labelId, pageToken, maxResults = PAGE_SIZE)
            addAll(page.messages.map { it.id })
            pageToken = page.nextPageToken
        } while (pageToken != null && size < limit)
    }.let { if (limit == Int.MAX_VALUE) it else it.take(limit) }

    private fun listWindow(
        labelId: String,
        startToken: String?,
        count: Int,
        query: String? = null,
    ): Pair<List<GmailRef>, String?> {
        val references = mutableListOf<GmailRef>()
        var pageToken = startToken
        while (references.size < count) {
            val page = api.listMessages(
                labelId,
                pageToken,
                maxResults = (count - references.size).coerceAtMost(PAGE_SIZE),
                query = query,
            )
            references += page.messages
            pageToken = page.nextPageToken
            if (pageToken == null) break
        }
        return references.take(count) to pageToken
    }

    private fun move(id: String, source: String, target: String): String {
        val add = if (target == ALL_MAIL_LABEL_ID) emptyList() else listOf(target)
        val remove = if (source == ALL_MAIL_LABEL_ID) emptyList() else listOf(source)
        if (add.isNotEmpty() || remove.isNotEmpty()) api.modify(id, add = add, remove = remove)
        return id
    }

    private fun partialMessageKey(id: String) = "$EXTRA_PARTIAL_PREFIX$id"

    private fun GmailMessage.belongsToFolder(folderServerId: String): Boolean =
        if (folderServerId == ALL_MAIL_LABEL_ID) {
            "TRASH" !in labelIds && "SPAM" !in labelIds
        } else {
            folderServerId in labelIds
        }

    private fun decodeReference(encoded: String): String? = runCatching {
        String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8)
    }.getOrNull()

    private object PollingPusher : BackendPusher {
        override fun start() = Unit
        override fun updateFolders(folderServerIds: Collection<String>) = Unit
        override fun stop() = Unit
        override fun reconnect() = Unit
    }

    private companion object {
        const val PAGE_SIZE = 500
        const val FETCH_MORE_BATCH_SIZE = 10
        const val MILLIS_PER_SECOND = 1000L
        const val ALL_MAIL_LABEL_ID = "ALL_MAIL"
        const val TRASH_LABEL_ID = "TRASH"
        const val EXTRA_VISIBLE_LIMIT = "gmail.visibleLimit"
        const val EXTRA_HISTORY_ID = "gmail.historyId"
        const val EXTRA_NEWEST_INTERNAL_DATE = "gmail.newestInternalDate"
        const val EXTRA_OLDER_PAGE_TOKEN = "gmail.olderPageToken"
        const val EXTRA_PARTIAL_PREFIX = "gmail.partial."
        const val ATTACHMENT_ID_HEADER = "X-Thunderbird-Gmail-Attachment-Id"
    }
}

/** Protocol identity used by account/backend selection; transport remains Gmail REST. */
object GmailApiProtocol {
    val name: String = Protocols.GMAIL_API
}

private fun String.toFolderType(): FolderType = when (this) {
    "INBOX" -> FolderType.INBOX
    "SENT" -> FolderType.SENT
    "DRAFT" -> FolderType.DRAFTS
    "TRASH" -> FolderType.TRASH
    "SPAM" -> FolderType.SPAM
    "ALL_MAIL" -> FolderType.ARCHIVE
    else -> FolderType.REGULAR
}
