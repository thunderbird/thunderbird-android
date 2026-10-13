package com.fsck.k9.backend.zohomail

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
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.Multipart
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeUtility
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.folder.api.FOLDER_DEFAULT_PATH_DELIMITER
import okhttp3.OkHttpClient

/** Native Zoho Mail REST backend. It intentionally has no IMAP/SMTP or push path. */
@Suppress("TooManyFunctions")
class ZohoMailBackend internal constructor(
    private val storage: BackendStorage,
    private val api: ZohoMailApiClient,
    private val incomingOnly: Boolean = false,
    private val messagePageLoader: (String, Int, Int) -> ZohoPage = { folderId, start, limit ->
        api.listMessages(folderId, start, limit)
    },
    private val attachmentInfoLoader: (String, String) -> List<ZohoAttachment> = { folderId, messageId ->
        api.attachmentInfo(folderId, messageId)
    },
) : Backend {
    constructor(
        storage: BackendStorage,
        tokenProvider: OAuth2TokenProvider,
        logger: Logger,
        verifiedMailApiOrigin: String,
        mailboxAddress: String,
        incomingOnly: Boolean = false,
    ) : this(
        storage,
        ZohoMailApiClient(OkHttpClient(), tokenProvider, logger, verifiedMailApiOrigin, mailboxAddress),
        incomingOnly,
    )

    @Volatile
    private var folderTypes: Map<String, FolderType>? = null

    override val supportsFlags = true
    override val supportsExpunge = false
    override val supportsMove = true
    override val supportsCopy = false
    override val supportsUpload = false
    override val savesSentMessagesToServer: Boolean get() = !incomingOnly
    override val supportsTrashFolder = true
    override val supportsSearchByDate = false
    override val supportsFolderSubscriptions = false
    override val isPushCapable = false

    override fun refreshFolderList(): String? {
        try {
            val remote = api.listFolders().filterNot { it.type.isNonSelectable() }
            val types = remote.associate { it.id to it.type.toFolderType() }
            folderTypes = types
            val remoteIds = remote.mapTo(mutableSetOf()) { it.id }
            val existingIds = storage.getFolderServerIds().toSet()
            storage.updateFolders {
                remote.filter { it.id in existingIds }.forEach {
                    changeFolder(it.id, it.name, it.type.toFolderType())
                }
                createFolders(
                    remote.filterNot { it.id in existingIds }.map {
                        FolderInfo(it.id, it.name, it.type.toFolderType(), FOLDER_DEFAULT_PATH_DELIMITER)
                    },
                )
                deleteFolders((existingIds - remoteIds).toList())
            }
            return FOLDER_DEFAULT_PATH_DELIMITER
        } catch (error: Exception) {
            throw MessagingException("Unable to refresh Zoho Mail folders", error)
        }
    }

    override fun sync(folderServerId: String, config: SyncConfig, listener: SyncListener) {
            listener.syncStarted(folderServerId)
        try {
            val folder = storage.getFolder(folderServerId)
            listener.syncHeadersStarted(folderServerId)
            val previousLimit = folder.getFolderExtraNumber(KEY_VISIBLE_LIMIT)?.toInt()
            val fetchMore =
                config.fetchMoreMessagesRequested &&
                    previousLimit != null &&
                    folder.visibleLimit > previousLimit

            if (fetchMore) {
                val start = folder.getFolderExtraNumber(KEY_OLDER_START)?.toInt() ?: 1
                val limit = FETCH_MORE_BATCH_SIZE
                val page = messagePageLoader(folderServerId, start, limit)
                val newlySaved = savePage(folder, folderServerId, page.messages, config, listener, oldMessages = true)
                folder.setFolderExtraNumber(KEY_OLDER_START, page.nextStart.toLong())
                folder.setFolderExtraNumber(KEY_VISIBLE_LIMIT, folder.visibleLimit.toLong())
                folder.setMoreMessages(if (page.hasMore) BackendFolder.MoreMessages.TRUE else BackendFolder.MoreMessages.FALSE)
                finishSync(folder, folderServerId, listener, newlySaved)
                return
            }

            if (previousLimit == null) {
                val wanted = (folder.visibleLimit.takeIf { it > 0 } ?: config.defaultVisibleLimit).coerceAtLeast(1)
                val initialSyncStartedAt = System.currentTimeMillis()
                val page = listWindow(folderServerId, 1, wanted + 1)
                val initial = page.messages.take(wanted)
                val newlySaved = savePage(folder, folderServerId, initial, config, listener, oldMessages = false)
                folder.setFolderExtraNumber(KEY_OLDER_START, (1 + initial.size).toLong())
                folder.setFolderExtraNumber(KEY_VISIBLE_LIMIT, folder.visibleLimit.toLong())
                folder.setFolderExtraNumber(
                    KEY_NEWEST_RECEIVED_TIME,
                    initial.mapNotNull { it.receivedTime }.maxOrNull() ?: initialSyncStartedAt,
                )
                val hasOlder = page.messages.size > initial.size || page.hasMore
                folder.setMoreMessages(if (hasOlder) BackendFolder.MoreMessages.TRUE else BackendFolder.MoreMessages.FALSE)
                finishSync(folder, folderServerId, listener, newlySaved)
                return
            }

            syncNewMessages(folder, folderServerId, config, listener)
        } catch (error: Exception) {
            listener.syncFailed(folderServerId, error.message ?: "Zoho Mail sync failed", error)
        }
    }

    private fun syncNewMessages(
        folder: BackendFolder,
        folderServerId: String,
        config: SyncConfig,
        listener: SyncListener,
    ) {
        var newestReceivedTime = folder.getFolderExtraNumber(KEY_NEWEST_RECEIVED_TIME)
        if (newestReceivedTime == null && folder.getMessageServerIds().isEmpty()) {
            newestReceivedTime = System.currentTimeMillis()
            folder.setFolderExtraNumber(KEY_NEWEST_RECEIVED_TIME, newestReceivedTime)
        }
        val baselineReceivedTime = newestReceivedTime
        var start = 1
        var added = 0
        var stopAtKnownMessage = false
        while (!stopAtKnownMessage) {
            val page = messagePageLoader(folderServerId, start, NEWER_REFRESH_PAGE_SIZE)
            if (page.messages.isEmpty()) break
            val result = processNewerPage(
                folder,
                folderServerId,
                config,
                listener,
                page,
                baselineReceivedTime,
                newestReceivedTime,
                added,
            )
            added = result.added
            newestReceivedTime = result.newestReceivedTime
            stopAtKnownMessage = result.stopAtKnownMessage
            if (stopAtKnownMessage || !page.hasMore) break
            start = page.nextStart
        }
        newestReceivedTime?.let { folder.setFolderExtraNumber(KEY_NEWEST_RECEIVED_TIME, it) }
        val olderStart = folder.getFolderExtraNumber(KEY_OLDER_START)
        if (olderStart != null && added > 0) {
            // Zoho paging uses a 1-based offset into a newest-first list. New arrivals shift older rows down.
            folder.setFolderExtraNumber(KEY_OLDER_START, olderStart + added)
        }
        folder.setFolderExtraNumber(KEY_VISIBLE_LIMIT, folder.visibleLimit.toLong())
        finishSync(folder, folderServerId, listener, added)
    }

    private fun processNewerPage(
        folder: BackendFolder,
        folderServerId: String,
        config: SyncConfig,
        listener: SyncListener,
        page: ZohoPage,
        baselineReceivedTime: Long?,
        newestReceivedTime: Long?,
        progressBase: Int,
    ): ZohoRefreshPageResult {
        var currentNewestReceivedTime = newestReceivedTime
        var added = progressBase
        var stopAtKnownMessage = false
        val progressTotal = progressBase + page.messages.size
        for ((index, summary) in page.messages.withIndex()) {
            if (folder.isMessagePresent(summary.id)) {
                syncFlags(folder, folderServerId, summary, config, listener)
                stopAtKnownMessage = true
                break
            }
            val receivedTime = summary.receivedTime
            if (baselineReceivedTime != null && receivedTime != null && receivedTime <= baselineReceivedTime) {
                stopAtKnownMessage = true
                break
            }
            save(folder, folderServerId, summary, config, MessageDownloadState.FULL)
            listener.syncNewMessage(folderServerId, summary.id, false)
            val completed = progressBase + index + 1
            listener.syncProgress(folderServerId, completed, progressTotal)
            syncFlags(folder, folderServerId, summary, config, listener)
            listener.syncHeadersProgress(folderServerId, completed, progressTotal)
            summary.receivedTime?.let { receivedTime ->
                if (currentNewestReceivedTime == null || receivedTime > currentNewestReceivedTime) {
                    currentNewestReceivedTime = receivedTime
                }
            }
            added++
        }
        return ZohoRefreshPageResult(added, currentNewestReceivedTime, stopAtKnownMessage)
    }

    private fun listWindow(folderServerId: String, start: Int, count: Int): ZohoMessageWindow {
        val messages = mutableListOf<ZohoSummary>()
        var nextStart = start
        var hasMore = true
        while (messages.size < count && hasMore) {
            val limit = (count - messages.size).coerceAtMost(MAX_PAGE_SIZE)
            val page = messagePageLoader(folderServerId, nextStart, limit)
            val pageMessages = page.messages.take(limit)
            if (pageMessages.isEmpty()) {
                hasMore = false
            } else {
                messages += pageMessages
                nextStart += pageMessages.size
                hasMore = page.hasMore && pageMessages.size == limit
            }
        }
        return ZohoMessageWindow(messages, hasMore)
    }

    private fun savePage(
        folder: BackendFolder,
        folderServerId: String,
        summaries: List<ZohoSummary>,
        config: SyncConfig,
        listener: SyncListener,
        oldMessages: Boolean,
    ): Int {
        var added = 0
        summaries.forEachIndexed { index, summary ->
            if (!folder.isMessagePresent(summary.id)) {
                val state = stateForSize(summary.size, config)
                save(folder, folderServerId, summary, config, state)
                listener.syncNewMessage(folderServerId, summary.id, oldMessages)
                added++
            }
            syncFlags(folder, folderServerId, summary, config, listener)
            listener.syncHeadersProgress(folderServerId, index + 1, summaries.size)
            listener.syncProgress(folderServerId, index + 1, summaries.size)
        }
        return added
    }

    private fun finishSync(folder: BackendFolder, folderServerId: String, listener: SyncListener, added: Int) {
        folder.setLastChecked(System.currentTimeMillis())
        listener.syncHeadersFinished(folderServerId, folder.getMessageServerIds().size, added)
        listener.syncAuthenticationSuccess()
        listener.syncFinished(folderServerId)
    }

    private fun stateForSize(size: Long?, config: SyncConfig): MessageDownloadState =
        if (size == null || size <= config.maximumAutoDownloadMessageSize.coerceAtLeast(0)) {
            MessageDownloadState.FULL
        } else {
            MessageDownloadState.PARTIAL
        }

    private fun save(
        folder: BackendFolder,
        folderId: String,
        summary: ZohoSummary,
        config: SyncConfig,
        requestedState: MessageDownloadState,
    ) {
        val state = if (requestedState == MessageDownloadState.PARTIAL) MessageDownloadState.PARTIAL else stateForSize(summary.size, config)
        val message = if (state == MessageDownloadState.PARTIAL) {
            val attachments = attachmentInfoLoader(folderId, summary.id)
            ZohoMailMimeFactory.createPartial(summary, attachments)
        } else {
            val details = api.getMessage(folderId, summary.id)
            val attachments = attachmentInfoLoader(folderId, summary.id)
            ZohoMailMimeFactory.create(details, summary, attachments)
        }
        runBlocking { folder.saveMessage(message, state) }
    }

    override fun downloadMessage(syncConfig: SyncConfig, folderServerId: String, messageServerId: String) {
        val folder = storage.getFolder(folderServerId)
        val summary = api.listMessages(folderServerId, start = 1, limit = MAX_PAGE_SIZE).messages
            .firstOrNull { it.id == messageServerId }
            ?: ZohoSummary(messageServerId, null, null, false, false, null, null, null, folderServerId)
        save(folder, folderServerId, summary, syncConfig, MessageDownloadState.FULL)
    }

    override fun downloadMessageStructure(folderServerId: String, messageServerId: String) {
        val folder = storage.getFolder(folderServerId)
        val summary = api.listMessages(folderServerId, start = 1, limit = MAX_PAGE_SIZE).messages
            .firstOrNull { it.id == messageServerId }
            ?: ZohoSummary(messageServerId, null, null, false, false, null, null, null, folderServerId)
        val attachments = attachmentInfoLoader(folderServerId, messageServerId)
        val message = ZohoMailMimeFactory.createEnvelope(summary, attachments)
        runBlocking { folder.saveMessage(message, MessageDownloadState.ENVELOPE) }
    }

    override suspend fun downloadCompleteMessage(folderServerId: String, messageServerId: String) {
        val mime = api.originalMessage(messageServerId)
        val parsed = MimeMessage.parseMimeMessage(ByteArrayInputStream(mime.toByteArray(StandardCharsets.UTF_8)), true).apply {
            uid = messageServerId
        }
        storage.getFolder(folderServerId).saveMessage(parsed, MessageDownloadState.FULL)
    }

    override fun setFlag(folderServerId: String, ids: List<String>, flag: Flag, newState: Boolean) {
        ids.forEach { id ->
            when (flag) {
                Flag.SEEN -> api.setRead(folderServerId, id, newState)
                Flag.FLAGGED -> api.setFlagged(folderServerId, id, newState)
                else -> throw UnsupportedOperationException("Zoho Mail does not support changing $flag")
            }
        }
    }

    override fun markAllAsRead(folderServerId: String) {
        forEachRemoteMessage(folderServerId) { api.setRead(folderServerId, it.id, true) }
    }

    override fun expunge(folderServerId: String) = Unit

    override fun deleteMessages(folderServerId: String, messageServerIds: List<String>) {
        val permanently = isTrashOrSpam(folderServerId)
        messageServerIds.forEach { api.delete(folderServerId, it, permanently) }
    }

    override fun deleteAllMessages(folderServerId: String) {
        val permanently = isTrashOrSpam(folderServerId)
        forEachRemoteMessage(folderServerId) { api.delete(folderServerId, it.id, permanently) }
    }

    override fun moveMessages(sourceFolderServerId: String, targetFolderServerId: String, messageServerIds: List<String>): Map<String, String> {
        messageServerIds.forEach { api.move(sourceFolderServerId, targetFolderServerId, it) }
        return messageServerIds.associateWith { it }
    }

    override fun moveMessagesAndMarkAsRead(sourceFolderServerId: String, targetFolderServerId: String, messageServerIds: List<String>): Map<String, String> {
        messageServerIds.forEach {
            api.move(sourceFolderServerId, targetFolderServerId, it)
            api.setRead(targetFolderServerId, it, true)
        }
        return messageServerIds.associateWith { it }
    }

    override fun copyMessages(sourceFolderServerId: String, targetFolderServerId: String, messageServerIds: List<String>): Map<String, String>? =
        throw UnsupportedOperationException("Zoho Mail REST does not expose a documented message-copy endpoint")

    override fun search(
        folderServerId: String,
        query: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        performFullTextSearch: Boolean,
    ): List<String> {
        if (query.isNullOrBlank()) return listFolderIds(folderServerId)
        val field = if (performFullTextSearch) "entire" else "subject"
        val escaped = query.replace("\\", "\\\\").replace("\"", "\\\"")
        val searchKey = "$field:\"$escaped\""
        val results = mutableListOf<ZohoSummary>()
        var start = 1
        while (true) {
            val page = api.searchMessages(searchKey, start, MAX_PAGE_SIZE)
            results += page.messages.filter { it.folderId == folderServerId }
            if (!page.hasMore) break
            start = page.nextStart
        }
        return results.asSequence()
            .filter { matchesFlags(it, requiredFlags, forbiddenFlags) }
            .map { it.id }
            .distinct()
            .toList()
    }

    override fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory) {
        val encodedReference = part.serverExtra ?: part.getHeader(ZohoMailMimeFactory.ATTACHMENT_ID_HEADER).firstOrNull()
            ?: throw MessagingException("Zoho Mail part has no lazy attachment reference")
        val attachmentId = ZohoMailMimeFactory.decodeReference(encodedReference)
            ?: throw MessagingException("Zoho Mail attachment reference is invalid")
        val bytes = if (part.disposition?.startsWith("inline", ignoreCase = true) == true && !part.contentId.isNullOrBlank()) {
            api.inlineBytes(folderServerId, messageServerId, part.contentId, ZohoMailMimeFactory.fileName(part))
        } else {
            api.attachmentBytes(folderServerId, messageServerId, attachmentId)
        }
        part.body = bodyFactory.createBody("binary", part.mimeType, ByteArrayInputStream(bytes))
        part.setHeader("Content-Transfer-Encoding", "binary")
    }

    override fun findByMessageId(folderServerId: String, messageId: String): String? = null

    override fun uploadMessage(folderServerId: String, message: Message): String? =
        throw UnsupportedOperationException("Zoho Mail REST draft/upload is unavailable without an explicit draft workflow")

    override fun sendMessage(message: Message) {
        if (incomingOnly) throw MessagingException("Zoho Mail account is configured for incoming email only")
        val compose = ZohoComposeMessage.from(message) { fileName, bytes, inline ->
            api.uploadAttachment(fileName, bytes, inline)
        }
        val inReplyTo = message.getHeader("In-Reply-To").firstOrNull()?.trim().orEmpty()
        if (inReplyTo.isBlank()) {
            api.send(compose.payload)
            return
        }
        val parentId = findReplyParent(message)
            ?: throw MessagingException("Zoho Mail could not resolve the original server message for a threaded reply")
        api.reply(parentId, compose.payload + ("action" to "Reply"))
    }

    override fun createPusher(callback: BackendPusherCallback): BackendPusher = PollingPusher

    private fun findReplyParent(message: Message): String? {
        val rfcMessageIds = buildList {
            message.getHeader("In-Reply-To").forEach(::add)
            message.references?.lastOrNull()?.let(::add)
        }.flatMap { value -> MESSAGE_ID_REGEX.findAll(value).map { it.groupValues[1] }.toList() }
            .distinct()
        if (rfcMessageIds.isEmpty()) return null
        val originalSubject = message.subject.orEmpty().replaceFirst(Regex("(?i)^\\s*re:\\s*"), "").trim()
        if (originalSubject.isBlank()) return null
        val escapedSubject = originalSubject.replace("\\", "\\\\").replace("\"", "\\\"")
        var start = 1
        var examined = 0
        while (examined < MAX_REPLY_SEARCH_RESULTS) {
            val page = api.searchMessages("subject:\"$escapedSubject\"", start, MAX_PAGE_SIZE)
            for (candidate in page.messages) {
                examined++
                val folderId = candidate.folderId ?: continue
                val rawHeaders = api.messageHeaders(folderId, candidate.id).orEmpty()
                val remoteMessageId = HEADER_MESSAGE_ID_REGEX.find(rawHeaders)?.groupValues?.get(1)?.trim()?.removePrefix("<")?.removeSuffix(">")
                if (remoteMessageId != null && rfcMessageIds.any { it.equals(remoteMessageId, ignoreCase = true) }) {
                    return candidate.id
                }
                if (examined >= MAX_REPLY_SEARCH_RESULTS) break
            }
            if (!page.hasMore) break
            start = page.nextStart
        }
        return null
    }

    private fun forEachRemoteMessage(folderServerId: String, block: (ZohoSummary) -> Unit) {
        var start = 1
        while (true) {
            val page = api.listMessages(folderServerId, start, MAX_PAGE_SIZE)
            page.messages.forEach(block)
            if (!page.hasMore) break
            start = page.nextStart
        }
    }

    private fun listFolderIds(folderServerId: String): List<String> = buildList {
        forEachRemoteMessage(folderServerId) { add(it.id) }
    }

    private fun isTrashOrSpam(folderServerId: String): Boolean {
        val current = folderTypes ?: synchronized(this) {
            folderTypes ?: api.listFolders().associate { it.id to it.type.toFolderType() }.also { folderTypes = it }
        }
        return current[folderServerId] in setOf(FolderType.TRASH, FolderType.SPAM)
    }

    private fun syncFlags(
        folder: BackendFolder,
        folderServerId: String,
        summary: ZohoSummary,
        config: SyncConfig,
        listener: SyncListener,
    ) {
        for (flag in config.syncFlags) {
            val remoteState = when (flag) {
                Flag.SEEN -> summary.read
                Flag.FLAGGED -> summary.flagged
                else -> continue
            }
            if (folder.getMessageFlags(summary.id).contains(flag) != remoteState) {
                folder.setMessageFlag(summary.id, flag, remoteState)
                listener.syncFlagChanged(folderServerId, summary.id)
            }
        }
    }

    private fun matchesFlags(summary: ZohoSummary, required: Set<Flag>?, forbidden: Set<Flag>?): Boolean {
        fun set(flag: Flag): Boolean = when (flag) {
            Flag.SEEN -> summary.read
            Flag.FLAGGED -> summary.flagged
            else -> false
        }
        return (required.orEmpty().all(::set)) && (forbidden.orEmpty().none(::set))
    }

    private fun forEachRemoteMessageInBatches(folderServerId: String, action: (String) -> Unit) =
        forEachRemoteMessage(folderServerId) { action(it.id) }

    private data class ZohoMessageWindow(val messages: List<ZohoSummary>, val hasMore: Boolean)
    private data class ZohoRefreshPageResult(
        val added: Int,
        val newestReceivedTime: Long?,
        val stopAtKnownMessage: Boolean,
    )

    private object PollingPusher : BackendPusher {
        override fun start() = Unit
        override fun updateFolders(folderServerIds: Collection<String>) = Unit
        override fun stop() = Unit
        override fun reconnect() = Unit
    }

    private companion object {
        const val MAX_PAGE_SIZE = 200
        const val FETCH_MORE_BATCH_SIZE = 10
        const val NEWER_REFRESH_PAGE_SIZE = 10
        const val MAX_REPLY_SEARCH_RESULTS = 2000
        const val KEY_VISIBLE_LIMIT = "zoho.visibleLimit"
        const val KEY_OLDER_START = "zoho.olderStart"
        const val KEY_NEWEST_RECEIVED_TIME = "zoho.newestReceivedTime"
        val MESSAGE_ID_REGEX = Regex("<([^<>]+)>")
        val HEADER_MESSAGE_ID_REGEX = Regex("(?im)^Message-ID:[ \\t]*<?([^>\\r\\n]+)>?")
    }
}

private fun String?.isNonSelectable(): Boolean = when (this?.trim()?.lowercase(Locale.ROOT)) {
    "templates", "template", "snoozed", "outbox" -> true
    else -> false
}

private fun String?.toFolderType(): FolderType = when (this?.trim()?.lowercase(Locale.ROOT)) {
    "inbox" -> FolderType.INBOX
    "sent" -> FolderType.SENT
    "draft", "drafts" -> FolderType.DRAFTS
    "trash", "bin" -> FolderType.TRASH
    "spam", "junk" -> FolderType.SPAM
    "archive", "archived" -> FolderType.ARCHIVE
    "outbox" -> FolderType.OUTBOX
    else -> FolderType.REGULAR
}

internal object ZohoMailMimeFactory {
    const val ATTACHMENT_ID_HEADER = "X-Thunderbird-Zoho-Attachment-Id"

    fun createEnvelope(summary: ZohoSummary): MimeMessage = createEnvelope(summary, emptyList())

    fun createEnvelope(summary: ZohoSummary, attachments: List<ZohoAttachment>): MimeMessage =
        parse(buildMime(summary, null, attachments), summary)

    fun createPartial(summary: ZohoSummary, attachments: List<ZohoAttachment>): MimeMessage =
        createEnvelope(summary, attachments)

    fun create(message: ZohoMessage, summary: ZohoSummary, attachments: List<ZohoAttachment>): MimeMessage =
        parse(buildMime(summary, message, attachments), summary)

    fun decodeReference(value: String): String? = runCatching {
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    }.getOrNull()

    fun fileName(part: Part): String? = extractParameter(part.disposition, "filename")
        ?: extractParameter(part.contentType, "name")

    private fun parse(raw: String, summary: ZohoSummary): MimeMessage {
        val parsed = MimeMessage.parseMimeMessage(ByteArrayInputStream(raw.toByteArray(StandardCharsets.UTF_8)), true).apply {
            uid = summary.id
            summary.receivedTime?.let { setInternalDate(Date(it)) }
            setFlags(buildSet {
                if (summary.read) add(Flag.SEEN)
                if (summary.flagged) add(Flag.FLAGGED)
            }, true)
        }
        removeLazyAttachmentBodies(parsed)
        return parsed
    }

    private fun buildMime(summary: ZohoSummary, details: ZohoMessage?, attachments: List<ZohoAttachment>): String = buildString {
        appendHeader("MIME-Version", "1.0")
        appendAddressHeaders(summary, details)
        summary.subject?.let { appendHeader("Subject", encodeHeader(it)) }
        val dateHeader = details?.dateHeader?.let(::cleanHeader)?.takeIf(String::isNotBlank)
        if (dateHeader != null) {
            appendHeader("Date", dateHeader)
        } else {
            (details?.sentTime ?: summary.receivedTime)?.let { sentTime ->
                val formattedDate = java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(
                    Date(sentTime).toInstant().atZone(java.time.ZoneOffset.UTC),
                )
                appendHeader("Date", formattedDate)
            }
        }
        summary.threadId?.let { appendHeader("X-Thunderbird-Zoho-Thread-Id", cleanHeader(it)) }
        val body = details?.content?.let(::rewriteInlineReferences).orEmpty()
        if (attachments.isEmpty()) {
            appendHeader("Content-Type", "text/html; charset=utf-8")
            appendHeader("Content-Transfer-Encoding", "base64")
            append("\r\n")
            appendBase64(body)
        } else {
            val boundary = "thunderbird-zoho-${java.util.UUID.randomUUID().toString().replace("-", "") }"
            appendHeader("Content-Type", "multipart/mixed; boundary=\"$boundary\"")
            append("\r\n--$boundary\r\n")
            append("Content-Type: text/html; charset=utf-8\r\nContent-Transfer-Encoding: base64\r\n\r\n")
            appendBase64(body)
            attachments.forEach { attachment ->
                append("--$boundary\r\n")
                val contentType = safeContentType(attachment.contentType, attachment.name, attachment.inline)
                append("Content-Type: $contentType\r\n")
                val fileName = safeFileName(attachment.name ?: "attachment")
                append("Content-Disposition: ${if (attachment.inline) "inline" else "attachment"}; ")
                append("filename=\"${fileName.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
                attachment.size?.takeIf { it >= 0 }?.let { append("; size=").append(it) }
                append("\r\n")
                if (attachment.contentId != null) {
                    val cid = cleanHeader(attachment.contentId).removePrefix("<").removeSuffix(">")
                    append("Content-ID: <$cid>\r\n")
                }
                val encodedId = Base64.getUrlEncoder().withoutPadding().encodeToString(attachment.id.toByteArray(StandardCharsets.UTF_8))
                append("$ATTACHMENT_ID_HEADER: $encodedId\r\n")
                append("Content-Transfer-Encoding: binary\r\n\r\n")
            }
            append("--$boundary--\r\n")
        }
    }

    private fun formatZohoAddressHeader(value: String, fallbackName: String? = null): String? {
        val decodedValue = decodeZohoAddressEntities(cleanHeader(value))
        val addresses = Address.parseUnencoded(decodedValue)
        if (addresses.isEmpty()) return null
        return addresses.joinToString(", ") { address ->
            val personal = address.personal?.takeIf(String::isNotBlank)
                ?: fallbackName?.let(::cleanHeader)?.let(::decodeZohoAddressEntities)?.takeIf(String::isNotBlank)
            cleanHeader(Address(address.address, personal, parse = false).toEncodedString())
        }
    }

    private fun StringBuilder.appendAddressHeaders(summary: ZohoSummary, details: ZohoMessage?) {
        val from = details?.from?.takeIf(String::isNotBlank) ?: summary.from?.takeIf(String::isNotBlank)
        val senderName = details?.sender?.takeIf(String::isNotBlank) ?: summary.sender?.takeIf(String::isNotBlank)
        from?.let { formatZohoAddressHeader(it, senderName) }?.let { appendHeader("From", it) }
        val to = details?.to?.takeIf(String::isNotBlank) ?: summary.to?.takeIf(String::isNotBlank)
        to?.let { formatZohoAddressHeader(it) }?.let { appendHeader("To", it) }
    }

    private fun decodeZohoAddressEntities(value: String): String = HTML_ENTITY_REGEX.replace(value) { match ->
        val entity = match.groupValues[1].lowercase(Locale.ROOT)
        when (entity) {
            "quot" -> "\""
            "apos", "#39" -> "'"
            "lt" -> "<"
            "gt" -> ">"
            "amp" -> "&"
            "nbsp" -> " "
            else -> {
                val codePoint = if (entity.startsWith("#x")) {
                    entity.substring(2).toIntOrNull(16)
                } else {
                    entity.removePrefix("#").toIntOrNull()
                }
                if (codePoint == null || !Character.isValidCodePoint(codePoint)) {
                    match.value
                } else {
                    String(Character.toChars(codePoint))
                }
            }
        }
    }

    private fun removeLazyAttachmentBodies(part: Part) {
        val multipart = part.body as? Multipart ?: return
        multipart.bodyParts.forEach { bodyPart ->
            val reference = bodyPart.getHeader(ATTACHMENT_ID_HEADER).firstOrNull()
            if (reference != null) {
                bodyPart.serverExtra = reference
                bodyPart.body = null
            } else {
                removeLazyAttachmentBodies(bodyPart)
            }
        }
    }

    private fun rewriteInlineReferences(value: String): String = value.replace(
        Regex("https?://[^\\\"'\\s>]*ImageDisplay\\?[^\\\"'\\s>]*?cid=([^&\\\"'\\s>]+)", RegexOption.IGNORE_CASE),
    ) { match -> "cid:${match.groupValues[1]}" }

    private fun StringBuilder.appendHeader(name: String, value: String) {
        append(name).append(": ").append(value).append("\r\n")
    }

    private fun StringBuilder.appendBase64(value: String) {
        append(Base64.getMimeEncoder(76, "\r\n".toByteArray(StandardCharsets.US_ASCII)).encodeToString(value.toByteArray(StandardCharsets.UTF_8)))
        append("\r\n")
    }

    private fun encodeHeader(value: String): String {
        val clean = cleanHeader(value)
        if (clean.all { it.code in 0x20..0x7e }) return clean
        val encoded = Base64.getEncoder().encodeToString(clean.toByteArray(StandardCharsets.UTF_8))
        return "=?UTF-8?B?$encoded?="
    }

    private fun cleanHeader(value: String): String = value.replace('\r', ' ').replace('\n', ' ').trim()

    private fun safeFileName(value: String): String = value
        .replace('/', '_')
        .replace('\\', '_')
        .replace('\r', '_')
        .replace('\n', '_')
        .ifBlank { "attachment" }

    private fun safeContentType(value: String?, name: String?, inline: Boolean): String {
        val candidate = value?.substringBefore(';')?.trim().orEmpty()
        if (CONTENT_TYPE_REGEX.matches(candidate)) return candidate
        val guessed = name?.let(URLConnection::guessContentTypeFromName)
            ?.takeIf { it != "application/octet-stream" && CONTENT_TYPE_REGEX.matches(it) }
        if (guessed != null) return guessed
        if (inline) {
            return when (name?.substringAfterLast('.', "")?.lowercase(Locale.ROOT)) {
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "svg" -> "image/svg+xml"
                else -> "image/jpeg"
            }
        }
        return "application/octet-stream"
    }

    private fun extractParameter(header: String?, name: String): String? {
        if (header == null) return null
        val match = Regex("(?:^|;)\\s*${Regex.escape(name)}\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^;\\s]*))", RegexOption.IGNORE_CASE).find(header)
            ?: return null
        return (match.groups[1]?.value ?: match.groups[2]?.value ?: match.groups[3]?.value)?.takeIf(String::isNotBlank)
    }

    private val CONTENT_TYPE_REGEX = Regex("[A-Za-z0-9!#$&^_.+-]+/[A-Za-z0-9!#$&^_.+-]+")
    private val HTML_ENTITY_REGEX = Regex(
        "&(#(?:x[0-9a-fA-F]+|[0-9]+)|quot|apos|lt|gt|amp|nbsp);",
        RegexOption.IGNORE_CASE,
    )
}

private data class ZohoOutgoingAttachment(val fileName: String, val inline: Boolean, val bytes: ByteArray)
private data class ZohoComposeContent(val plainText: String?, val html: String?, val attachments: List<ZohoOutgoingAttachment>)

private object ZohoComposeMessage {
    fun from(message: Message, upload: (String, ByteArray, Boolean) -> ZohoUploadedAttachment): ZohoComposedMessage {
        val content = collect(message)
        val from = message.from.orEmpty().firstOrNull()?.address.orEmpty()
        if (from.isBlank()) throw MessagingException("Zoho Mail send requires a From address")
        val payload = linkedMapOf<String, Any?>(
            "fromAddress" to from,
            "toAddress" to message.getRecipients(Message.RecipientType.TO).joinToString(",") { it.address },
            "subject" to message.subject.orEmpty(),
            "content" to (content.html ?: content.plainText.orEmpty()),
            "mailFormat" to if (content.html != null) "html" else "plaintext",
            "encoding" to "UTF-8",
        )
        message.getRecipients(Message.RecipientType.CC).joinToString(",") { it.address }.takeIf(String::isNotBlank)?.let { payload["ccAddress"] = it }
        message.getRecipients(Message.RecipientType.BCC).joinToString(",") { it.address }.takeIf(String::isNotBlank)?.let { payload["bccAddress"] = it }
        if (message.getHeader("Disposition-Notification-To").isNotEmpty()) payload["askReceipt"] = "yes"
        if (content.attachments.isNotEmpty()) {
            payload["attachments"] = content.attachments.map { attachment ->
                val uploaded = upload(attachment.fileName, attachment.bytes, attachment.inline)
                mapOf("attachmentName" to uploaded.name, "attachmentPath" to uploaded.path, "storeName" to uploaded.storeName)
            }
        }
        return ZohoComposedMessage(payload)
    }

    private fun collect(message: Message): ZohoComposeContent {
        val plain = mutableListOf<String>()
        val html = mutableListOf<String>()
        val attachments = mutableListOf<ZohoOutgoingAttachment>()
        collectPart(message, plain, html, attachments)
        return ZohoComposeContent(plain.takeIf { it.isNotEmpty() }?.joinToString("\n"), html.takeIf { it.isNotEmpty() }?.joinToString("\n"), attachments)
    }

    private fun collectPart(part: Part, plain: MutableList<String>, html: MutableList<String>, attachments: MutableList<ZohoOutgoingAttachment>) {
        val body = part.body ?: return
        if (body is Multipart) {
            body.bodyParts.forEach { collectPart(it, plain, html, attachments) }
            return
        }
        val disposition = part.disposition.orEmpty()
        val filename = extractFileName(part)
        val isAttachment = disposition.startsWith("attachment", true) || filename != null || part.contentId != null
        val bytes = body.inputStream.use { it.readBytes() }
        if (!isAttachment && part.isMimeType("text/html")) {
            html += String(bytes, StandardCharsets.UTF_8)
        } else if (!isAttachment && part.isMimeType("text/plain")) {
            plain += String(bytes, StandardCharsets.UTF_8)
        } else if (isAttachment || !part.isMimeType("text/plain") && !part.isMimeType("text/html")) {
            if (bytes.isEmpty()) throw MessagingException("Zoho Mail cannot send an empty attachment")
            val inferredName = filename ?: "attachment"
            val inline = disposition.startsWith("inline", true) || part.contentId != null
            attachments += ZohoOutgoingAttachment(inferredName, inline, bytes)
        }
    }

    private fun extractFileName(part: Part): String? = parameter(part.disposition, "filename") ?: parameter(part.contentType, "name")

    private fun parameter(header: String?, name: String): String? {
        if (header == null) return null
        val match = Regex("(?:^|;)\\s*${Regex.escape(name)}\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^;\\s]*))", RegexOption.IGNORE_CASE).find(header)
            ?: return null
        return (match.groups[1]?.value ?: match.groups[2]?.value ?: match.groups[3]?.value)?.takeIf(String::isNotBlank)
    }

}

private data class ZohoComposedMessage(val payload: Map<String, Any?>)
