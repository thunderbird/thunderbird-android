package com.fsck.k9.backend.imap

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.api.BackendPusher
import com.fsck.k9.backend.api.BackendPusherCallback
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.power.PowerManager
import com.fsck.k9.mail.store.imap.IdleRefreshManager
import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.transport.smtp.SmtpTransport

class ImapBackend(
    private val accountName: String,
    backendStorage: BackendStorage,
    internal val imapStore: ImapStore,
    private val powerManager: PowerManager,
    private val idleRefreshManager: IdleRefreshManager,
    private val pushConfigProvider: ImapPushConfigProvider,
    private val smtpTransport: SmtpTransport,
) : Backend {
    private val imapSync = ImapSync(accountName, backendStorage, imapStore)
    private val commandRefreshFolderList = CommandRefreshFolderList(backendStorage, imapStore)
    private val commandSetFlag = CommandSetFlag(imapStore)
    private val commandMarkAllAsRead = CommandMarkAllAsRead(imapStore)
    private val commandExpunge = CommandExpunge(imapStore)
    private val commandMoveOrCopyMessages = CommandMoveOrCopyMessages(imapStore)
    private val commandDelete = CommandDelete(imapStore)
    private val commandDeleteAll = CommandDeleteAll(imapStore)
    private val commandSearch = CommandSearch(imapStore)
    private val commandDownloadMessage = CommandDownloadMessage(backendStorage, imapStore)
    private val commandFetchMessage = CommandFetchMessage(imapStore)
    private val commandFindByMessageId = CommandFindByMessageId(imapStore)
    private val commandUploadMessage = CommandUploadMessage(imapStore)

    override val supportsFlags = true
    override val supportsExpunge = true
    override val supportsMove = true
    override val supportsCopy = true
    override val supportsUpload = true
    override val supportsTrashFolder = true
    override val supportsSearchByDate = true
    override val supportsFolderSubscriptions = true
    override val isPushCapable = true

    override fun refreshFolderList() {
        commandRefreshFolderList.refreshFolderList()
    }

    override fun sync(folderServerId: String, syncConfig: SyncConfig, listener: SyncListener) {
        imapSync.sync(folderServerId, syncConfig, listener)
    }

    override fun downloadMessage(syncConfig: SyncConfig, folderServerId: String, messageServerId: String) {
        imapSync.downloadMessage(syncConfig, folderServerId, messageServerId)
    }

    override fun downloadMessageStructure(folderServerId: String, messageServerId: String) {
        commandDownloadMessage.downloadMessageStructure(folderServerId, messageServerId)
    }

    override fun downloadCompleteMessage(folderServerId: String, messageServerId: String) {
        commandDownloadMessage.downloadCompleteMessage(folderServerId, messageServerId)
    }

    override fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        commandSetFlag.setFlag(folderServerId, messageServerIds, flag, newState)
    }

    override fun markAllAsRead(folderServerId: String) {
        commandMarkAllAsRead.markAllAsRead(folderServerId)
    }

    override fun expunge(folderServerId: String) {
        commandExpunge.expunge(folderServerId)
    }

    override fun deleteMessages(folderServerId: String, messageServerIds: List<String>) {
        commandDelete.deleteMessages(folderServerId, messageServerIds)
    }

    override fun deleteAllMessages(folderServerId: String) {
        commandDeleteAll.deleteAll(folderServerId)
    }

    override fun moveMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String>? {
        return commandMoveOrCopyMessages.moveMessages(sourceFolderServerId, targetFolderServerId, messageServerIds)
    }

    override fun moveMessagesAndMarkAsRead(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String>? {
        val uidMapping = commandMoveOrCopyMessages.moveMessages(
            sourceFolderServerId,
            targetFolderServerId,
            messageServerIds,
        )
        if (uidMapping != null) {
            setFlag(targetFolderServerId, uidMapping.values.toList(), Flag.SEEN, true)
        }
        return uidMapping
    }

    override fun copyMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String>? {
        return commandMoveOrCopyMessages.copyMessages(sourceFolderServerId, targetFolderServerId, messageServerIds)
    }

    override fun search(
        folderServerId: String,
        query: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        performFullTextSearch: Boolean,
    ): List<String> {
        return commandSearch.search(folderServerId, query, requiredFlags, forbiddenFlags, performFullTextSearch)
    }

    override fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory) {
        commandFetchMessage.fetchPart(folderServerId, messageServerId, part, bodyFactory)
    }

    override fun findByMessageId(folderServerId: String, messageId: String): String? {
        return commandFindByMessageId.findByMessageId(folderServerId, messageId)
    }

    override fun uploadMessage(folderServerId: String, message: Message): String? {
        return commandUploadMessage.uploadMessage(folderServerId, message)
    }

    override fun sendMessage(message: Message) {
        smtpTransport.sendMessage(message)
    }

    override fun createPusher(callback: BackendPusherCallback): BackendPusher {
        return ImapBackendPusher(imapStore, powerManager, idleRefreshManager, pushConfigProvider, callback, accountName)
    }
}
