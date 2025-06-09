package app.k9mail.legacy.mailstore

import java.util.concurrent.CopyOnWriteArraySet
import net.thunderbird.feature.mail.folder.api.FolderDetails

@Suppress("TooManyFunctions")
class ListenableMessageStore(private val messageStore: MessageStore) : MessageStore by messageStore {
    private val folderSettingsListener = CopyOnWriteArraySet<FolderSettingsChangedListener>()

    override fun createFolders(folders: List<CreateFolderInfo>): Set<Long> {
        return messageStore.createFolders(folders).also {
            notifyFolderSettingsChanged()
        }
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

    override fun setVisible(folderId: Long, visible: Boolean) {
        messageStore.setVisible(folderId, visible)
        notifyFolderSettingsChanged()
    }

    override fun setSyncEnabled(folderId: Long, enable: Boolean) {
        messageStore.setSyncEnabled(folderId, enable)
        notifyFolderSettingsChanged()
    }

    override fun setPushEnabled(folderId: Long, enable: Boolean) {
        messageStore.setPushEnabled(folderId, enable)
        notifyFolderSettingsChanged()
    }

    override fun setNotificationsEnabled(folderId: Long, enable: Boolean) {
        messageStore.setNotificationsEnabled(folderId, enable)
        notifyFolderSettingsChanged()
    }

    override fun setPushDisabled() {
        messageStore.setPushDisabled()
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
