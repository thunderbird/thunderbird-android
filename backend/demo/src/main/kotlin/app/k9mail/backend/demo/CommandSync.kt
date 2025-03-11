package app.k9mail.backend.demo

import com.fsck.k9.backend.api.BackendFolder.MoreMessages
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.mail.MessageDownloadState

internal class CommandSync(
    private val backendStorage: BackendStorage,
    private val demoStore: DemoStore,
) {

    fun sync(folderServerId: String, listener: SyncListener) {
        listener.syncStarted(folderServerId)

        val folder = demoStore.getFolder(folderServerId)
        if (folder == null) {
            listener.syncFailed(folderServerId, "Folder $folderServerId doesn't exist", null)
            return
        }

        val backendFolder = backendStorage.getFolder(folderServerId)

        val localMessageServerIds = backendFolder.getMessageServerIds()
        if (localMessageServerIds.isNotEmpty()) {
            listener.syncFinished(folderServerId)
            return
        }

        for (messageServerId in folder.messageServerIds) {
            val message = demoStore.getMessage(folderServerId, messageServerId)
            backendFolder.saveMessage(message, MessageDownloadState.FULL)
            listener.syncNewMessage(folderServerId, messageServerId, isOldMessage = false)
        }

        backendFolder.setMoreMessages(MoreMessages.FALSE)

        listener.syncFinished(folderServerId)
    }
}
