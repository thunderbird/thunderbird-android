package app.k9mail.legacy.mailstore

import app.k9mail.legacy.folder.FolderDetails
import com.fsck.k9.mail.FolderClass
import java.util.concurrent.CopyOnWriteArraySet

@Suppress("TooManyFunctions")
class ListenableMessageStore(private val messageStore: MessageStore) : MessageStore by messageStore {
    private val folderSettingsListener = CopyOnWriteArraySet<FolderSettingsChangedListener>()

    override fun createFolders(folders: List<CreateFolderInfo>) {
        messageStore.createFolders(folders)
        notifyFolderSettingsChanged()
    }

    override fun deleteFolders(folderServerIds: List<String>) {
        messageStore.deleteFolders(folderServerIds)
        notifyFolderSettingsChanged()
    }

    override fun updateFolderSettings(folderDetails: FolderDetails) {
        messageStore.updateFolderSettings(folderDetails)
        notifyFolderSettingsChanged()
    }

    override fun setIncludeInUnifiedInbox(folderId: Long, includeInUnifiedInbox: Boolean) {
        messageStore.setIncludeInUnifiedInbox(folderId, includeInUnifiedInbox)
        notifyFolderSettingsChanged()
    }

    override fun setDisplayClass(folderId: Long, folderClass: FolderClass) {
        messageStore.setDisplayClass(folderId, folderClass)
        notifyFolderSettingsChanged()
    }

    override fun setSyncClass(folderId: Long, folderClass: FolderClass) {
        messageStore.setSyncClass(folderId, folderClass)
        notifyFolderSettingsChanged()
    }

    override fun setPushClass(folderId: Long, folderClass: FolderClass) {
        messageStore.setPushClass(folderId, folderClass)
        notifyFolderSettingsChanged()
    }

    override fun setNotificationClass(folderId: Long, folderClass: FolderClass) {
        messageStore.setNotificationClass(folderId, folderClass)
        notifyFolderSettingsChanged()
    }

    fun addFolderSettingsChangedListener(listener: FolderSettingsChangedListener) {
        folderSettingsListener.add(listener)
    }

    fun removeFolderSettingsChangedListener(listener: FolderSettingsChangedListener) {
        folderSettingsListener.remove(listener)
    }

    private fun notifyFolderSettingsChanged() {
        for (listener in folderSettingsListener) {
            listener.onFolderSettingsChanged()
        }
    }
}

fun interface FolderSettingsChangedListener {
    fun onFolderSettingsChanged()
}
