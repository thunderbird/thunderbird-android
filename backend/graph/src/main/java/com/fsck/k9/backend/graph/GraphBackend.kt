package com.fsck.k9.backend.graph

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
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.internet.MimeMessage
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Date
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.folder.api.FOLDER_DEFAULT_PATH_DELIMITER
import okhttp3.OkHttpClient

/** Polling Microsoft Graph mail backend. */
@Suppress("TooManyFunctions")
class GraphBackend internal constructor(
    private val backendStorage: BackendStorage,
    private val api: GraphApiClient,
) : Backend {
    constructor(backendStorage: BackendStorage, tokenProvider: OAuth2TokenProvider, logger: Logger) : this(
        backendStorage = backendStorage,
        api = GraphApiClient(OkHttpClient(), tokenProvider, logger),
    )

    override val supportsFlags = true
    override val supportsExpunge = false
    override val supportsMove = true
    override val supportsCopy = true
    override val supportsUpload = false
    override val savesSentMessagesToServer = true
    override val supportsTrashFolder = true
    override val supportsSearchByDate = false
    override val supportsFolderSubscriptions = false
    override val isPushCapable = false

    @Suppress("TooGenericExceptionCaught")
    override fun refreshFolderList(): String? {
        try {
            val remoteFolders = api.listFolders()
            val remoteIds = remoteFolders.mapTo(mutableSetOf()) { it.id }
            val oldIds = backendStorage.getFolderServerIds().toSet()
            backendStorage.updateFolders {
                remoteFolders.filter { it.id in oldIds }.forEach { folder ->
                    changeFolder(folder.id, folder.path, folder.toFolderType())
                }
                val newFolders = remoteFolders.filterNot { it.id in oldIds }.map { folder ->
                    FolderInfo(
                        serverId = folder.id,
                        name = folder.path,
                        type = folder.toFolderType(),
                        folderPathDelimiter = FOLDER_DEFAULT_PATH_DELIMITER,
                    )
                }
                createFolders(newFolders)
                deleteFolders((oldIds - remoteIds).toList())
            }
            return FOLDER_DEFAULT_PATH_DELIMITER
        } catch (e: Exception) {
            throw MessagingException(
                "Unable to refresh Microsoft Graph folders",
                e,
            )
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun sync(folderServerId: String, syncConfig: SyncConfig, listener: SyncListener) {
        listener.syncStarted(folderServerId)
        try {
            val folder = backendStorage.getFolder(folderServerId)
            listener.syncHeadersStarted(folderServerId)
            val deltaLink = folder.getFolderExtraString(EXTRA_DELTA_LINK)
            val previousVisibleLimit = folder.getFolderExtraString(EXTRA_VISIBLE_LIMIT)?.toIntOrNull()
            val hasReceivedTimeBaseline =
                folder.getFolderExtraString(EXTRA_NEWEST_RECEIVED_TIME)?.toInstantOrNull() != null
            val isFetchMore = syncConfig.fetchMoreMessagesRequested && deltaLink != null &&
                previousVisibleLimit != null &&
                folder.visibleLimit > previousVisibleLimit

            val result = when {
                deltaLink == null -> initialSync(folder, folderServerId, syncConfig, listener, previousVisibleLimit)
                isFetchMore -> fetchOlderPage(
                    folder,
                    folderServerId,
                    previousVisibleLimit,
                    syncConfig,
                    listener,
                )
                else -> try {
                    if (hasReceivedTimeBaseline) {
                        refreshFromDelta(folder, folderServerId, syncConfig, listener, deltaLink)
                    } else {
                        rebaselineLegacyFolder(
                            folder,
                            folderServerId,
                            previousVisibleLimit,
                            syncConfig,
                            listener,
                            deltaLink,
                        )
                    }
                } catch (_: GraphDeltaExpiredException) {
                    // A stale delta link can be re-established from a recent bounded window.
                    folder.setFolderExtraString(EXTRA_DELTA_LINK, null)
                    initialSync(folder, folderServerId, syncConfig, listener, previousVisibleLimit)
                }
            }

            folder.setFolderExtraString(EXTRA_VISIBLE_LIMIT, folder.visibleLimit.toString())
            folder.setLastChecked(System.currentTimeMillis())
            listener.syncHeadersFinished(
                folderServerId,
                folder.getMessageServerIds().size,
                result.newMessageIds.size,
            )
            listener.syncAuthenticationSuccess()
            listener.syncFinished(folderServerId)
        } catch (e: Exception) {
            listener.syncFailed(folderServerId, e.message ?: "Microsoft Graph sync failed", e)
        }
    }

    override fun downloadMessage(syncConfig: SyncConfig, folderServerId: String, messageServerId: String) {
        val folder = backendStorage.getFolder(folderServerId)
        if (!folder.isMessagePresent(messageServerId)) {
            saveFullMessage(folder, folderServerId, messageServerId)
        }
    }

    override fun downloadMessageStructure(folderServerId: String, messageServerId: String) {
        // Message structure and attachment metadata are stored during the bounded message fetch.
    }

    override suspend fun downloadCompleteMessage(folderServerId: String, messageServerId: String) {
        // The body is already stored; attachment bytes are fetched only through fetchPart().
    }

    override fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        when (flag) {
            Flag.SEEN -> messageServerIds.forEach { api.setMessageRead(it, newState) }
            Flag.FLAGGED -> messageServerIds.forEach { api.setMessageFlagged(it, newState) }
            else -> unsupported("changing the $flag flag")
        }
    }

    override fun markAllAsRead(folderServerId: String) {
        api.listMessageIds(folderServerId, unreadOnly = true).forEach { api.setMessageRead(it, true) }
    }

    override fun expunge(folderServerId: String) {
        unsupported("expunging messages")
    }

    override fun deleteMessages(folderServerId: String, messageServerIds: List<String>) {
        messageServerIds.forEach(api::deleteMessage)
    }

    override fun deleteAllMessages(folderServerId: String) {
        api.listMessageIds(folderServerId).forEach(api::deleteMessage)
    }

    override fun moveMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String>? = messageServerIds.associateWith { messageServerId ->
        api.moveMessage(messageServerId, targetFolderServerId)
    }

    override fun moveMessagesAndMarkAsRead(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String>? = messageServerIds.associateWith { messageServerId ->
        api.moveMessage(messageServerId, targetFolderServerId).also { movedMessageId ->
            api.setMessageRead(movedMessageId, true)
        }
    }

    override fun copyMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String>? = messageServerIds.associateWith { messageServerId ->
        api.copyMessage(messageServerId, targetFolderServerId)
    }

    override fun search(
        folderServerId: String,
        query: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        performFullTextSearch: Boolean,
    ): List<String> = unsupported("server-side search")

    override fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory) {
        val encodedId = part.serverExtra
            ?: part.getHeader(GraphMessageMimeFactory.ATTACHMENT_ID_HEADER).firstOrNull()
            ?: unsupported("fetching a part that is not a Graph attachment")
        val attachmentId = GraphMessageMimeFactory.decodeAttachmentId(encodedId)
            ?: throw net.thunderbird.core.common.exception.MessagingException("Invalid Graph attachment reference")
        val metadataSize = part.getHeader(GraphMessageMimeFactory.ATTACHMENT_SIZE_HEADER)
            .firstOrNull()
            ?.toLongOrNull()
            ?.takeIf { it >= 0 }
        val body = api.downloadAttachment(
            messageServerId,
            attachmentId,
            part.contentType,
            bodyFactory,
            metadataSize,
        )
        part.body = body
        part.setHeader("Content-Transfer-Encoding", "binary")
    }

    override fun findByMessageId(folderServerId: String, messageId: String): String? = null

    override fun uploadMessage(folderServerId: String, message: Message): String? =
        throw MessagingException(
            "Microsoft Graph mail integration does not support uploading messages yet",
            isPermanentFailure = true,
        )

    override fun sendMessage(message: Message) {
        api.sendMessage(message)
    }

    override fun createPusher(callback: BackendPusherCallback): BackendPusher = PollingOnlyBackendPusher

    private fun initialSync(
        folder: BackendFolder,
        folderServerId: String,
        syncConfig: SyncConfig,
        listener: SyncListener,
        initialLimitOverride: Int? = null,
    ): SyncResult {
        val bootstrapStartedAt = Instant.now()
        val initialMessageLimit = initialLimitOverride?.takeIf { it > 0 } ?: folder.visibleLimit.takeIf { it > 0 }
            ?: syncConfig.defaultVisibleLimit.coerceAtLeast(1)
        val latestPage = api.getLatestMessages(folderServerId, initialMessageLimit)
        val newestMessages = latestPage.values.take(initialMessageLimit)
        val initialMessageIds = newestMessages.mapTo(mutableSetOf()) { it.id }
        val initialBaseline = newestMessages.mapNotNull { it.receivedDateTime.toInstantOrNull() }
            .maxOrNull() ?: bootstrapStartedAt
        val cutoff = newestMessages.mapNotNull { it.receivedDateTime.toInstantOrNull() }
            .minOrNull() ?: Instant.now().minus(1, ChronoUnit.DAYS)
        val candidates = linkedMapOf<String, GraphMessage>()
        newestMessages.filterNot { it.removed }.forEach { candidates[it.id] = it }

        var page = api.getMessageDeltaPage(folderServerId, cutoff)
        var finalDeltaLink: String? = null
        while (true) {
            page.values.filterNot { it.removed }.forEach { candidates[it.id] = it }
            if (page.nextLink != null) {
                page = api.getMessagePage(page.nextLink)
            } else {
                finalDeltaLink = page.deltaLink
                break
            }
        }

        val newMessageIds = saveNewMessages(
            folder = folder,
            folderServerId = folderServerId,
            messageServerIds = newestMessages.filterNot { it.removed }.map { it.id },
            listener = listener,
            syncConfig = syncConfig,
        )
        val pendingIds = candidates.values.asSequence()
            .filterNot { it.removed || it.id in initialMessageIds || folder.isMessagePresent(it.id) }
            .filter { message -> message.receivedDateTime.toInstantOrNull()?.isAfter(initialBaseline) == true }
            .map { it.id }
            .toList()
        folder.setFolderExtraString(EXTRA_PENDING_IDS, pendingIds.encodePendingIds())

        folder.setFolderExtraString(EXTRA_OLDER_LINK, latestPage.nextLink)
        folder.setFolderExtraString(EXTRA_PENDING_OLDER_IDS, null)
        folder.setFolderExtraString(EXTRA_NEWEST_RECEIVED_TIME, initialBaseline.toString())
        val moreMessages = if (latestPage.nextLink == null) {
            BackendFolder.MoreMessages.FALSE
        } else {
            BackendFolder.MoreMessages.TRUE
        }
        folder.setMoreMessages(moreMessages)
        folder.setFolderExtraString(
            EXTRA_DELTA_LINK,
            finalDeltaLink ?: throw GraphApiException(
                GRAPH_HTTP_BAD_GATEWAY,
                "Microsoft Graph did not return a delta link",
            ),
        )
        return SyncResult(newMessageIds)
    }

    private fun rebaselineLegacyFolder(
        folder: BackendFolder,
        folderServerId: String,
        previousVisibleLimit: Int?,
        syncConfig: SyncConfig,
        listener: SyncListener,
        deltaLink: String,
    ): SyncResult {
        val bootstrapStartedAt = Instant.now()
        val initialMessageLimit = previousVisibleLimit?.takeIf { it > 0 }
            ?: folder.visibleLimit.takeIf { it > 0 }
            ?: syncConfig.defaultVisibleLimit.coerceAtLeast(1)
        val latestPage = api.getLatestMessages(folderServerId, initialMessageLimit)
        val newestMessages = latestPage.values.take(initialMessageLimit).filterNot { it.removed }
        val existingBoundary = newestMessages.firstOrNull { folder.isMessagePresent(it.id) }
        val baseline = existingBoundary?.receivedDateTime?.toInstantOrNull()
            ?: newestMessages.mapNotNull { it.receivedDateTime.toInstantOrNull() }.maxOrNull()
            ?: bootstrapStartedAt
        val newerThanKnownBoundary = existingBoundary?.let { boundary ->
            newestMessages.takeWhile { it.id != boundary.id }
                .filter { it.receivedDateTime.toInstantOrNull()?.isAfter(baseline) == true }
        }.orEmpty()
        val newMessageIds = saveNewMessages(
            folder = folder,
            folderServerId = folderServerId,
            messageServerIds = newerThanKnownBoundary.map { it.id },
            listener = listener,
            syncConfig = syncConfig,
        )
        folder.setFolderExtraString(EXTRA_NEWEST_RECEIVED_TIME, baseline.toString())
        folder.setFolderExtraString(EXTRA_OLDER_LINK, latestPage.nextLink)
        folder.setMoreMessages(
            if (latestPage.nextLink == null) BackendFolder.MoreMessages.FALSE else BackendFolder.MoreMessages.TRUE,
        )

        val deltaResult = refreshFromDelta(folder, folderServerId, syncConfig, listener, deltaLink)
        return SyncResult(newMessageIds + deltaResult.newMessageIds)
    }

    private fun fetchOlderPage(
        folder: BackendFolder,
        folderServerId: String,
        previousVisibleLimit: Int,
        syncConfig: SyncConfig,
        listener: SyncListener,
    ): SyncResult {
        val requestedMessageCount = if (folder.visibleLimit > previousVisibleLimit) FETCH_MORE_BATCH_SIZE else 0
        val existingIds = folder.getMessageServerIds()
        val selectedIds = mutableListOf<String>()
        val selectedIdSet = mutableSetOf<String>()
        val deferredIds = mutableListOf<String>()
        val pendingIds = folder.getFolderExtraString(EXTRA_PENDING_OLDER_IDS).decodePendingIds()
        pendingIds.forEach { messageId ->
            if (messageId in existingIds || !selectedIdSet.add(messageId)) return@forEach
            if (selectedIds.size < requestedMessageCount) {
                selectedIds += messageId
            } else {
                deferredIds += messageId
            }
        }

        var olderLink = folder.getFolderExtraString(EXTRA_OLDER_LINK)
        if (requestedMessageCount == 0 || (olderLink == null && selectedIds.isEmpty())) {
            folder.setMoreMessages(BackendFolder.MoreMessages.FALSE)
            folder.setFolderExtraString(EXTRA_PENDING_OLDER_IDS, deferredIds.encodePendingIds())
            return SyncResult(emptySet())
        }

        while (selectedIds.size < requestedMessageCount && olderLink != null) {
            val page = api.getMessagePage(olderLink)
            olderLink = page.nextLink
            val pageMessageIds = page.values.asSequence()
                .filterNot { it.removed }
                .map { it.id }
                .distinct()
                .filter { it !in existingIds && it !in selectedIdSet }
                .toList()
            val remainingCount = requestedMessageCount - selectedIds.size
            val acceptedIds = pageMessageIds.take(remainingCount)
            selectedIds += acceptedIds
            selectedIdSet += acceptedIds
            deferredIds += pageMessageIds.drop(acceptedIds.size)
        }

        val newMessageIds = saveNewMessages(folder, folderServerId, selectedIds, listener, syncConfig)
        folder.setFolderExtraString(EXTRA_OLDER_LINK, olderLink)
        folder.setFolderExtraString(EXTRA_PENDING_OLDER_IDS, deferredIds.encodePendingIds())
        folder.setMoreMessages(
            if (olderLink != null || deferredIds.isNotEmpty()) {
                BackendFolder.MoreMessages.TRUE
            } else {
                BackendFolder.MoreMessages.FALSE
            },
        )
        return SyncResult(newMessageIds)
    }

    private fun refreshFromDelta(
        folder: BackendFolder,
        folderServerId: String,
        syncConfig: SyncConfig,
        listener: SyncListener,
        initialDeltaLink: String,
    ): SyncResult {
        val deltaChanges = readAllDeltaChanges(initialDeltaLink)
        val changedMessages = deltaChanges.changedMessages
        val removedIds = deltaChanges.removedIds

        val existingIds = folder.getMessageServerIds()
        val deletions = if (syncConfig.syncRemoteDeletions) removedIds.intersect(existingIds) else emptySet()
        if (deletions.isNotEmpty()) {
            folder.destroyMessages(deletions.toList())
            deletions.forEach { listener.syncRemovedMessage(folderServerId, it) }
        }

        val baselineReceivedTime = folder.getFolderExtraString(EXTRA_NEWEST_RECEIVED_TIME)?.toInstantOrNull() ?: Instant.now()
        var newestReceivedTime = baselineReceivedTime
        val pendingIds = folder.getFolderExtraString(EXTRA_PENDING_IDS).decodePendingIds()
        val pendingToImport = pendingIds.filterNot { it in existingIds || it in removedIds || it in changedMessages }
        val pendingMessages = pendingToImport.mapNotNull { messageId ->
            api.getMessage(folderServerId, messageId).takeIf { message ->
                message.receivedDateTime.toInstantOrNull()?.isAfter(baselineReceivedTime) == true
            }
        }
        val newChangedMessages = changedMessages.values.filter { message ->
            message.id !in existingIds &&
                message.receivedDateTime.toInstantOrNull()?.isAfter(baselineReceivedTime) == true
        }
        val newChangedMessageIds = newChangedMessages.mapTo(mutableSetOf()) { it.id }
        val progressTotal = pendingMessages.size + newChangedMessages.size
        val newMessageIds = mutableSetOf<String>()
        var completed = 0
        pendingMessages.forEach { message ->
            saveFullMessage(folder, folderServerId, message)
            newMessageIds += message.id
            listener.syncNewMessage(
                folderServerId,
                message.id,
                message.receivedDateTime.isOlderThan(syncConfig.earliestPollDate),
            )
            message.receivedDateTime.toInstantOrNull()?.let { receivedTime ->
                if (receivedTime.isAfter(newestReceivedTime)) newestReceivedTime = receivedTime
            }
            listener.syncProgress(folderServerId, ++completed, progressTotal)
        }
        changedMessages.values.forEach { summary ->
            if (summary.id in existingIds) {
                syncFlags(folder, folderServerId, summary, syncConfig, listener)
            } else if (summary.id in newChangedMessageIds) {
                val message = saveFullMessage(folder, folderServerId, summary.id)
                newMessageIds += summary.id
                listener.syncNewMessage(
                    folderServerId,
                    summary.id,
                    summary.receivedDateTime.isOlderThan(syncConfig.earliestPollDate),
                )
                message.receivedDateTime.toInstantOrNull()?.let { receivedTime ->
                    if (receivedTime.isAfter(newestReceivedTime)) newestReceivedTime = receivedTime
                }
                listener.syncProgress(folderServerId, ++completed, progressTotal)
            }
        }
        folder.setFolderExtraString(EXTRA_NEWEST_RECEIVED_TIME, newestReceivedTime.toString())
        folder.setFolderExtraString(
            EXTRA_DELTA_LINK,
            deltaChanges.deltaLink,
        )
        folder.setFolderExtraString(EXTRA_PENDING_IDS, null)
        return SyncResult(newMessageIds)
    }

    private fun readAllDeltaChanges(initialDeltaLink: String): DeltaChanges {
        val changedMessages = linkedMapOf<String, GraphMessage>()
        val removedIds = mutableSetOf<String>()
        var page = api.getMessagePage(initialDeltaLink)
        while (true) {
            page.values.forEach { message ->
                if (message.removed) {
                    removedIds += message.id
                    changedMessages.remove(message.id)
                } else {
                    changedMessages[message.id] = message
                    removedIds.remove(message.id)
                }
            }
            val nextLink = page.nextLink
            if (nextLink != null) {
                page = api.getMessagePage(nextLink)
            } else {
                val deltaLink = page.deltaLink ?: throw GraphApiException(
                    GRAPH_HTTP_BAD_GATEWAY,
                    "Microsoft Graph did not return a delta link",
                )
                return DeltaChanges(changedMessages, removedIds, deltaLink)
            }
        }
    }

    private fun saveNewMessages(
        folder: BackendFolder,
        folderServerId: String,
        messageServerIds: List<String>,
        listener: SyncListener,
        syncConfig: SyncConfig,
    ): Set<String> {
        val existingIds = folder.getMessageServerIds()
        val newMessageServerIds = messageServerIds.distinct().filterNot { it in existingIds }
        val newMessageIds = mutableSetOf<String>()
        newMessageServerIds.forEachIndexed { index, messageServerId ->
            val message = saveFullMessage(folder, folderServerId, messageServerId)
            newMessageIds += messageServerId
            listener.syncNewMessage(
                folderServerId,
                messageServerId,
                message.receivedDateTime.isOlderThan(syncConfig.earliestPollDate),
            )
            listener.syncHeadersProgress(folderServerId, index + 1, newMessageServerIds.size)
            listener.syncProgress(folderServerId, index + 1, newMessageServerIds.size)
        }
        return newMessageIds
    }

    private fun saveFullMessage(
        folder: BackendFolder,
        folderServerId: String,
        messageServerId: String,
    ): GraphMessage = saveFullMessage(folder, folderServerId, api.getMessage(folderServerId, messageServerId))

    private fun saveFullMessage(
        folder: BackendFolder,
        folderServerId: String,
        message: GraphMessage,
    ): GraphMessage {
        val attachments = if (message.hasAttachments) api.listAttachments(message.id) else emptyList()
        val mimeMessage: MimeMessage = GraphMessageMimeFactory.create(message, attachments)
        runBlocking { folder.saveMessage(mimeMessage, MessageDownloadState.FULL) }
        return message
    }

    private fun syncFlags(
        folder: BackendFolder,
        folderServerId: String,
        summary: GraphMessage,
        syncConfig: SyncConfig,
        listener: SyncListener,
    ) {
        val remoteFlags = buildSet {
            if (summary.isRead) add(Flag.SEEN)
            if (summary.isFlagged) add(Flag.FLAGGED)
        }
        var changed = false
        for (flag in syncConfig.syncFlags) {
            if (flag != Flag.SEEN && flag != Flag.FLAGGED) continue
            val remoteValue = flag in remoteFlags
            val localValue = flag in folder.getMessageFlags(summary.id)
            if (remoteValue != localValue) {
                folder.setMessageFlag(summary.id, flag, remoteValue)
                changed = true
            }
        }
        if (changed) listener.syncFlagChanged(folderServerId, summary.id)
    }

    private fun GraphMailFolder.toFolderType(): com.fsck.k9.mail.FolderType = when (wellKnownName) {
        "inbox" -> com.fsck.k9.mail.FolderType.INBOX
        "sentitems" -> com.fsck.k9.mail.FolderType.SENT
        "drafts" -> com.fsck.k9.mail.FolderType.DRAFTS
        "deleteditems" -> com.fsck.k9.mail.FolderType.TRASH
        "junkemail" -> com.fsck.k9.mail.FolderType.SPAM
        "archive" -> com.fsck.k9.mail.FolderType.ARCHIVE
        else -> com.fsck.k9.mail.FolderType.REGULAR
    }

    private fun String?.toInstantOrNull(): Instant? = runCatching { this?.let(Instant::parse) }.getOrNull()

    private fun String?.isOlderThan(earliestPollDate: Date?): Boolean {
        val received = toInstantOrNull() ?: return false
        return earliestPollDate?.let { received.isBefore(it.toInstant()) } ?: false
    }

    private fun Collection<String>.encodePendingIds(): String? = map { id ->
        Base64.getUrlEncoder().withoutPadding().encodeToString(id.toByteArray(StandardCharsets.UTF_8))
    }.joinToString("\n").takeIf(String::isNotEmpty)

    private fun String?.decodePendingIds(): Set<String> = this.orEmpty().lineSequence()
        .filter(String::isNotBlank)
        .mapNotNull { encoded ->
            runCatching { String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8) }.getOrNull()
        }
        .toSet()

    private fun unsupported(operation: String): Nothing =
        throw UnsupportedOperationException("Microsoft Graph mail integration does not support $operation yet")

    private data class SyncResult(val newMessageIds: Set<String>)

    private data class DeltaChanges(
        val changedMessages: Map<String, GraphMessage>,
        val removedIds: Set<String>,
        val deltaLink: String,
    )

    private object PollingOnlyBackendPusher : BackendPusher {
        override fun start() = Unit
        override fun updateFolders(folderServerIds: Collection<String>) = Unit
        override fun stop() = Unit
        override fun reconnect() = Unit
    }

    private companion object {
        const val FETCH_MORE_BATCH_SIZE = 10
        const val EXTRA_DELTA_LINK = "graphDeltaLink"
        const val EXTRA_OLDER_LINK = "graphOlderMessagesLink"
        const val EXTRA_VISIBLE_LIMIT = "graphVisibleLimit"
        const val EXTRA_PENDING_IDS = "graphPendingMessageIds"
        const val EXTRA_NEWEST_RECEIVED_TIME = "graphNewestReceivedTime"
        const val EXTRA_PENDING_OLDER_IDS = "graphPendingOlderMessageIds"
    }
}
