package com.fsck.k9.mailstore

import android.content.ContentValues
import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendFolderUpdater
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.FolderType as RemoteFolderType

class K9BackendStorage(
    private val localStore: LocalStore,
    private val messageStore: MessageStore,
    private val folderSettingsProvider: FolderSettingsProvider,
    private val listeners: List<BackendFoldersRefreshListener>
) : BackendStorage {
    private val database = localStore.database

    override fun getFolder(folderServerId: String): BackendFolder {
        return K9BackendFolder(localStore, folderServerId)
    }

    override fun getFolderServerIds(): List<String> {
        return messageStore.getFolders(excludeLocalOnly = true) { folder -> folder.serverId }
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
                    settings = folderSettingsProvider.getFolderSettings(folderInfo.serverId)
                )
            }
            localStore.createFolders(createFolderInfo)
        }

        override fun deleteFolders(folderServerIds: List<String>) {
            folderServerIds.asSequence()
                .map { localStore.getFolder(it) }
                .forEach { it.delete() }
        }

        override fun changeFolder(folderServerId: String, name: String, type: RemoteFolderType) {
            database.execute(false) { db ->
                val values = ContentValues().apply {
                    put("name", name)
                    put("type", type.toDatabaseFolderType())
                }

                db.update("folders", values, "server_id = ?", arrayOf(folderServerId))
            }
        }

        override fun close() {
            listeners.forEach { it.onAfterFolderListRefresh() }
        }
    }
}
