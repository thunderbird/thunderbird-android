package com.fsck.k9.backend.eas

import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.Folder

const val EXTRA_FOLDER_SYNC_KEY = "EXTRA_FOLDER_SYNC_KEY"

class EasFolderSyncCommand(private val client: EasClient,
                           private val provisionManager: EasProvisionManager,
                           private val backendStorage: BackendStorage) {

    fun sync() {
        val syncKey = backendStorage.getExtraString(EXTRA_FOLDER_SYNC_KEY) ?: "0"

        provisionManager.ensureProvisioned {
            val folderSyncResponse = client.folderSync(FolderSync(syncKey = syncKey))
            val folderChanges = folderSyncResponse?.folderChanges!!
            val newSyncKey = folderSyncResponse.syncKey
            backendStorage.setExtraString(EXTRA_FOLDER_SYNC_KEY, newSyncKey)

            if (folderChanges.folderAdd != null) {
                backendStorage.createFolders(
                        folderChanges.folderAdd.map {
                            FolderInfo(it.serverID, it.name, it.getFolderType())
                        })
            }

            if (folderChanges.folderDelete != null) {
                backendStorage.deleteFolders(folderChanges.folderDelete.map { it.serverID })
            }

            folderChanges.folderUpdate?.forEach {
                backendStorage.changeFolder(it.serverID, it.name, it.getFolderType())
            }
        }
    }

    private fun FolderChange.getFolderType() = when (folderType) {
        2 -> Folder.FolderType.INBOX
        3 -> Folder.FolderType.DRAFTS
        4 -> Folder.FolderType.TRASH
        5 -> Folder.FolderType.SENT
        6 -> Folder.FolderType.OUTBOX
        else -> Folder.FolderType.REGULAR
    }

}
