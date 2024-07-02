package com.fsck.k9.mailstore

import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendFolderUpdater
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.FolderType as RemoteFolderType

class K9BackendStorage(
    private val messageStore: MessageStore,
    private val folderSettingsProvider: FolderSettingsProvider,
    private val saveMessageDataCreator: SaveMessageDataCreator,
    private val listeners: List<BackendFoldersRefreshListener>,
) : BackendStorage {
    override fun getFolder(folderServerId: String): BackendFolder {
        return K9BackendFolder(messageStore, saveMessageDataCreator, folderServerId)
    }

    override fun getFolderServerIds(): List<String> {
        return messageStore.getFolders(excludeLocalOnly = true) { folder -> folder.serverIdOrThrow() }
    }

    override fun createFolderUpdater(): BackendFolderUpdater {
        return K9BackendFolderUpdater()
    }

    override fun getExtraString(name: String): String? {
        return messageStore.getExtraString(name)
    }

    override fun setExtraString(name: String, value: String) {
        messageStore.setExtraString(name, value)
    }

    override fun getExtraNumber(name: String): Long? {
        return messageStore.getExtraNumber(name)
    }

    override fun setExtraNumber(name: String, value: Long) {
        messageStore.setExtraNumber(name, value)
    }

    private inner class K9BackendFolderUpdater : BackendFolderUpdater {
        init {
            listeners.forEach { it.onBeforeFolderListRefresh() }
        }

        override fun createFolders(folders: List<FolderInfo>) {
            if (folders.isEmpty()) return

            val createFolderInfo = folders.map { folderInfo ->
                CreateFolderInfo(
                    serverId = folderInfo.serverId,
                    name = folderInfo.name,
                    type = folderInfo.type,
                    settings = folderSettingsProvider.getFolderSettings(folderInfo.serverId),
                )
            }
            messageStore.createFolders(createFolderInfo)
        }

        override fun deleteFolders(folderServerIds: List<String>) {
            if (folderServerIds.isNotEmpty()) {
                messageStore.deleteFolders(folderServerIds)
            }
        }

        override fun changeFolder(folderServerId: String, name: String, type: RemoteFolderType) {
            messageStore.changeFolder(folderServerId, name, type)
        }

        override fun close() {
            listeners.forEach { it.onAfterFolderListRefresh() }
        }
    }
}
