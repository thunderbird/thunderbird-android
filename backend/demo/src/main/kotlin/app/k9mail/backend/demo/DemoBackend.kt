package app.k9mail.backend.demo

import app.k9mail.backend.demo.DemoHelper.createNewServerId
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
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter

class DemoBackend(
    private val backendStorage: BackendStorage,
) : Backend {
    private val demoStore by lazy { DemoStore() }

    private val commandSync by lazy { CommandSync(backendStorage, demoStore) }
    private val commandRefreshFolderList by lazy { CommandRefreshFolderList(backendStorage, demoStore) }
    private val commandSendMessage by lazy { CommandSendMessage(backendStorage, demoStore) }

    override val supportsFlags: Boolean = true
    override val supportsExpunge: Boolean = false
    override val supportsMove: Boolean = true
    override val supportsCopy: Boolean = true
    override val supportsUpload: Boolean = true
    override val supportsTrashFolder: Boolean = true
    override val supportsSearchByDate: Boolean = false
    override val supportsFolderSubscriptions: Boolean = false
    override val isPushCapable: Boolean = false

    override fun refreshFolderList(): FolderPathDelimiter? {
        return commandRefreshFolderList.refreshFolderList()
    }

    override fun sync(folderServerId: String, syncConfig: SyncConfig, listener: SyncListener) {
        commandSync.sync(folderServerId, listener)
    }

    override fun downloadMessage(syncConfig: SyncConfig, folderServerId: String, messageServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun downloadMessageStructure(folderServerId: String, messageServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun downloadCompleteMessage(folderServerId: String, messageServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) = Unit

    override fun markAllAsRead(folderServerId: String) = Unit

    override fun expunge(folderServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun deleteMessages(folderServerId: String, messageServerIds: List<String>) = Unit

    override fun deleteAllMessages(folderServerId: String) = Unit

    override fun moveMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String> {
        // We do just enough to simulate a successful operation on the server.
        return messageServerIds.associateWith { createNewServerId() }
    }

    override fun moveMessagesAndMarkAsRead(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String> {
        // We do just enough to simulate a successful operation on the server.
        return messageServerIds.associateWith { createNewServerId() }
    }

    override fun copyMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String> {
        // We do just enough to simulate a successful operation on the server.
        return messageServerIds.associateWith { createNewServerId() }
    }

    override fun search(
        folderServerId: String,
        query: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        performFullTextSearch: Boolean,
    ): List<String> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun findByMessageId(folderServerId: String, messageId: String): String? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun uploadMessage(folderServerId: String, message: Message): String {
        return createNewServerId()
    }

    override fun sendMessage(message: Message) {
        commandSendMessage.sendMessage(message)
    }

    override fun createPusher(callback: BackendPusherCallback): BackendPusher {
        throw UnsupportedOperationException("not implemented")
    }
}
