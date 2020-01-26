package com.fsck.k9.backend.webdav

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.PushReceiver
import com.fsck.k9.mail.Pusher
import com.fsck.k9.mail.store.webdav.WebDavStore
import com.fsck.k9.mail.transport.WebDavTransport
import timber.log.Timber

class WebDavBackend(
    accountName: String,
    backendStorage: BackendStorage,
    private val webDavStore: WebDavStore,
    private val webDavTransport: WebDavTransport
) : Backend {
    private val webDavSync: WebDavSync = WebDavSync(accountName, backendStorage, webDavStore)
    private val commandGetFolders = CommandRefreshFolderList(backendStorage, webDavStore)
    private val commandSetFlag = CommandSetFlag(webDavStore)
    private val commandMoveOrCopyMessages = CommandMoveOrCopyMessages(webDavStore)
    private val commandFetchMessage = CommandFetchMessage(webDavStore)
    private val commandUploadMessage = CommandUploadMessage(webDavStore)

    override val supportsSeenFlag = true
    override val supportsExpunge = true
    override val supportsMove = true
    override val supportsCopy = true
    override val supportsUpload = true
    override val supportsTrashFolder = true
    override val supportsSearchByDate = false
    override val isPushCapable = false
    override val isDeleteMoveToTrash = true

    override fun refreshFolderList() {
        commandGetFolders.refreshFolderList()
    }

    override fun sync(folder: String, syncConfig: SyncConfig, listener: SyncListener) {
        webDavSync.sync(folder, syncConfig, listener)
    }

    override fun downloadMessage(syncConfig: SyncConfig, folderServerId: String, messageServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    @Throws(MessagingException::class)
    override fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        commandSetFlag.setFlag(folderServerId, messageServerIds, flag, newState)
    }

    override fun markAllAsRead(folderServerId: String) {
        Timber.e("Method not implemented; breaks 'mark all as read'")
    }

    override fun expunge(folderServerId: String) {
        throw UnsupportedOperationException("not supported")
    }

    override fun expungeMessages(folderServerId: String, messageServerIds: List<String>) {
        throw UnsupportedOperationException("not supported")
    }

    override fun deleteMessages(folderServerId: String, messageServerIds: List<String>) {
        commandSetFlag.setFlag(folderServerId, messageServerIds, Flag.DELETED, true)
    }

    override fun deleteAllMessages(folderServerId: String) {
        Timber.e("Method not implemented; breaks 'empty trash'")
    }

    override fun moveMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>
    ): Map<String, String>? {
        return commandMoveOrCopyMessages.moveMessages(sourceFolderServerId, targetFolderServerId, messageServerIds)
    }

    override fun moveMessagesAndMarkAsRead(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>
    ): Map<String, String>? {
        val uidMapping = commandMoveOrCopyMessages
                .moveMessages(sourceFolderServerId, targetFolderServerId, messageServerIds)
        if (uidMapping != null) {
            setFlag(targetFolderServerId, uidMapping.values.toList(), Flag.SEEN, true)
        }
        return uidMapping
    }

    override fun copyMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>
    ): Map<String, String>? {
        return commandMoveOrCopyMessages.copyMessages(sourceFolderServerId, targetFolderServerId, messageServerIds)
    }

    override fun search(
        folderServerId: String,
        query: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?
    ): List<String> {
        throw UnsupportedOperationException("not supported")
    }

    override fun fetchMessage(folderServerId: String, messageServerId: String, fetchProfile: FetchProfile): Message {
        return commandFetchMessage.fetchMessage(folderServerId, messageServerId, fetchProfile)
    }

    override fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory) {
        throw UnsupportedOperationException("not supported")
    }

    override fun findByMessageId(folderServerId: String, messageId: String): String? {
        return null
    }

    override fun uploadMessage(folderServerId: String, message: Message): String? {
        return commandUploadMessage.uploadMessage(folderServerId, message)
    }

    override fun createPusher(receiver: PushReceiver): Pusher {
        throw UnsupportedOperationException("not supported")
    }

    override fun checkIncomingServerSettings() {
        webDavStore.checkSettings()
    }

    override fun sendMessage(message: Message) {
        webDavTransport.sendMessage(message)
    }

    override fun checkOutgoingServerSettings() {
        webDavTransport.checkSettings()
    }
}
