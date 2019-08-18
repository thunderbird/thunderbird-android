package com.fsck.k9.backend.eas

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.mail.*
import com.fsck.k9.mail.ssl.TrustManagerFactory

class EasBackend(backendStorage: BackendStorage,
                 trustManagerFactory: TrustManagerFactory,
                 easServerSettings: EasServerSettings,
                 deviceId: String) : Backend {
    override val supportsSeenFlag = true
    override val supportsExpunge = false
    override val supportsMove = false
    override val supportsCopy = false
    override val supportsUpload = false
    override val supportsTrashFolder = false
    override val supportsSearchByDate = false
    override val isPushCapable = false
    override val isDeleteMoveToTrash = false

    private val client = EasClient(easServerSettings, trustManagerFactory, deviceId)

    private val provisionManager = EasProvisionManager(client, backendStorage)

    private val folderSyncCommand = EasFolderSyncCommand(client, provisionManager, backendStorage)
    private val syncCommand = EasSyncCommand(client, provisionManager, backendStorage)
    private val messageFetchCommand = EasMessageFetchCommand(client, provisionManager, backendStorage)

    override fun refreshFolderList() {
        folderSyncCommand.sync()
    }

    override fun sync(folder: String, syncConfig: SyncConfig, listener: SyncListener, providedRemoteFolder: Folder<*>?) {
        syncCommand.sync(folder, syncConfig, listener)
    }

    override fun downloadMessage(syncConfig: SyncConfig, folderServerId: String, messageServerId: String) {
        //messageFetchCommand.fetch(folderServerId, messageServerId)
    }

    override fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        syncCommand.setFlag(folderServerId, messageServerIds, flag, newState)
    }

    override fun markAllAsRead(folderServerId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun expunge(folderServerId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun expungeMessages(folderServerId: String, messageServerIds: List<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteMessages(folderServerId: String, messageServerIds: List<String>) {
        syncCommand.delete(folderServerId, messageServerIds)
    }

    override fun deleteAllMessages(folderServerId: String) {
        // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun moveMessages(sourceFolderServerId: String, targetFolderServerId: String, messageServerIds: List<String>): Map<String, String>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun copyMessages(sourceFolderServerId: String, targetFolderServerId: String, messageServerIds: List<String>): Map<String, String>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun search(folderServerId: String, query: String?, requiredFlags: Set<Flag>?, forbiddenFlags: Set<Flag>?): List<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fetchMessage(folderServerId: String, messageServerId: String, fetchProfile: FetchProfile): Message {
        return messageFetchCommand.fetch(folderServerId, messageServerId)
    }

    override fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findByMessageId(folderServerId: String, messageId: String): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun uploadMessage(folderServerId: String, message: Message): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createPusher(receiver: PushReceiver): Pusher {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun checkIncomingServerSettings() {
        client.initialize()
    }

    override fun sendMessage(message: Message) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun checkOutgoingServerSettings() {
        client.initialize()
    }
}
