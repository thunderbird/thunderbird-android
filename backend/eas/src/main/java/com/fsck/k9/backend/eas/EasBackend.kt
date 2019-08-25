package com.fsck.k9.backend.eas

import com.fsck.k9.mail.power.PowerManager
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.mail.*
import com.fsck.k9.mail.ssl.TrustManagerFactory

class EasBackend(backendStorage: BackendStorage,
                 trustManagerFactory: TrustManagerFactory,
                 easServerSettings: EasServerSettings,
                 private val powerManager: PowerManager,
                 deviceId: String) : Backend {
    override val supportsSeenFlag = true
    override val supportsExpunge = false
    override val supportsMove = true
    override val supportsCopy = false
    override val supportsUpload = false
    override val supportsTrashFolder = true
    override val supportsSearchByDate = true
    override val supportsSearchByVisibleLimit = false
    override val isPushCapable = true
    override val isDeleteMoveToTrash = true

    private val client = EasClient(easServerSettings, trustManagerFactory, deviceId)

    private val provisionManager = EasProvisionManager(client, backendStorage)

    private val folderSyncCommand = FolderSyncCommand(client, provisionManager, backendStorage)
    private val syncCommand = SyncCommand(client, provisionManager, backendStorage)
    private val messageMoveCommand = MessageMoveCommand(client, provisionManager)
    private val sendMessageCommand = MessageSendCommand(client, provisionManager)

    override fun refreshFolderList() {
        folderSyncCommand.sync()
    }

    override fun sync(folder: String, syncConfig: SyncConfig, listener: SyncListener, providedRemoteFolder: Folder<*>?) {
        syncCommand.sync(folder, syncConfig, listener)
    }

    override fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        syncCommand.setFlag(folderServerId, messageServerIds, flag, newState)
    }

    override fun markAllAsRead(folderServerId: String) {
        // not supported
    }

    override fun deleteMessages(folderServerId: String, messageServerIds: List<String>) {
        // not supported
    }

    override fun expungeMessages(folderServerId: String, messageServerIds: List<String>) {
        throw UnsupportedOperationException("not supported")
    }

    override fun deleteAllMessages(folderServerId: String) {
        // not supported
    }

    override fun expunge(folderServerId: String) {
        throw UnsupportedOperationException("not supported")
    }

    override fun moveMessages(sourceFolderServerId: String, targetFolderServerId: String, messageServerIds: List<String>): Map<String, String>? {
        return messageMoveCommand.moveMessages(sourceFolderServerId, targetFolderServerId, messageServerIds)
    }

    override fun copyMessages(sourceFolderServerId: String, targetFolderServerId: String, messageServerIds: List<String>): Map<String, String>? {
        throw UnsupportedOperationException("not supported")
    }

    override fun search(folderServerId: String, query: String?, requiredFlags: Set<Flag>?, forbiddenFlags: Set<Flag>?): List<String> {
        throw UnsupportedOperationException("not supported")
    }

    override fun downloadMessage(syncConfig: SyncConfig, folderServerId: String, messageServerId: String) {
        //messageFetchCommand.fetch(folderServerId, messageServerId)
    }

    override fun fetchMessage(folderServerId: String, messageServerId: String, fetchProfile: FetchProfile): Message {
        return syncCommand.fetch(folderServerId, messageServerId)
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

    override fun createPusher(receiver: PushReceiver) = EasPusher(client, powerManager, receiver)

    override fun sendMessage(message: Message) {
        sendMessageCommand.sendMessage(message)
    }

    override fun checkIncomingServerSettings() {
        client.initialize()
    }

    override fun checkOutgoingServerSettings() {
        client.initialize()
    }
}
