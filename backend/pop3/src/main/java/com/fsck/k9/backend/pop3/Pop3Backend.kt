package com.fsck.k9.backend.pop3

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.api.BackendPusher
import com.fsck.k9.backend.api.BackendPusherCallback
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.store.pop3.Pop3Store
import com.fsck.k9.mail.transport.smtp.SmtpTransport
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter

class Pop3Backend(
    accountName: String,
    backendStorage: BackendStorage,
    private val pop3Store: Pop3Store,
    private val smtpTransport: SmtpTransport,
) : Backend {
    private val pop3Sync: Pop3Sync = Pop3Sync(accountName, backendStorage, pop3Store)
    private val commandRefreshFolderList = CommandRefreshFolderList(backendStorage)
    private val commandSetFlag = CommandSetFlag(pop3Store)
    private val commandDownloadMessage = CommandDownloadMessage(backendStorage, pop3Store)

    override val supportsFlags = false
    override val supportsExpunge = false
    override val supportsMove = false
    override val supportsCopy = false
    override val supportsUpload = false
    override val supportsTrashFolder = false
    override val supportsSearchByDate = false
    override val supportsFolderSubscriptions = false
    override val isPushCapable = false

    override fun refreshFolderList(): FolderPathDelimiter? {
        commandRefreshFolderList.refreshFolderList()
        return null
    }

    override fun sync(folderServerId: String, syncConfig: SyncConfig, listener: SyncListener) {
        pop3Sync.sync(folderServerId, syncConfig, listener)
    }

    override fun downloadMessage(syncConfig: SyncConfig, folderServerId: String, messageServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun downloadMessageStructure(folderServerId: String, messageServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun downloadCompleteMessage(folderServerId: String, messageServerId: String) {
        commandDownloadMessage.downloadCompleteMessage(folderServerId, messageServerId)
    }

    override fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        commandSetFlag.setFlag(folderServerId, messageServerIds, flag, newState)
    }

    override fun markAllAsRead(folderServerId: String) {
        throw UnsupportedOperationException("not supported")
    }

    override fun expunge(folderServerId: String) {
        throw UnsupportedOperationException("not supported")
    }

    override fun deleteMessages(folderServerId: String, messageServerIds: List<String>) {
        commandSetFlag.setFlag(folderServerId, messageServerIds, Flag.DELETED, true)
    }

    override fun deleteAllMessages(folderServerId: String) {
        throw UnsupportedOperationException("not supported")
    }

    override fun moveMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String>? {
        throw UnsupportedOperationException("not supported")
    }

    override fun copyMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String>? {
        throw UnsupportedOperationException("not supported")
    }

    override fun moveMessagesAndMarkAsRead(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String>? {
        throw UnsupportedOperationException("not supported")
    }

    override fun search(
        folderServerId: String,
        query: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        performFullTextSearch: Boolean,
    ): List<String> {
        throw UnsupportedOperationException("not supported")
    }

    override fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory) {
        throw UnsupportedOperationException("not supported")
    }

    override fun findByMessageId(folderServerId: String, messageId: String): String? {
        return null
    }

    override fun uploadMessage(folderServerId: String, message: Message): String? {
        throw UnsupportedOperationException("not supported")
    }

    override fun sendMessage(message: Message) {
        smtpTransport.sendMessage(message)
    }

    override fun createPusher(callback: BackendPusherCallback): BackendPusher {
        throw UnsupportedOperationException("not implemented")
    }
}
