package com.fsck.k9.backend.eas

import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.eas.dto.FolderChange
import com.fsck.k9.backend.eas.dto.FolderSync
import com.fsck.k9.mail.Folder
import com.fsck.k9.mail.MessagingException

class FolderSyncCommand(private val client: EasClient,
                        private val provisionManager: EasProvisionManager,
                        private val backendStorage: BackendStorage) {

    val EXTRA_FOLDER_SYNC_KEY = "EXTRA_FOLDER_SYNC_KEY"

    val FOLDER_TYPE_GENERIC = 1
    val FOLDER_TYPE_INBOX = 2
    val FOLDER_TYPE_DRAFS = 3
    val FOLDER_TYPE_TRASH = 4
    val FOLDER_TYPE_SENT = 5
    val FOLDER_TYPE_OUTBOX = 6
    val FOLDER_TYPE_EMAIL_USER_CREATED = 12
    val FOLDER_TYPE_UNKNOWN = 18

    fun sync() {
        val syncKey = backendStorage.getExtraString(EXTRA_FOLDER_SYNC_KEY) ?: INITIAL_SYNC_KEY

        provisionManager.ensureProvisioned {
            val folderSyncResponse = client.folderSync(FolderSync(syncKey = syncKey))

            if (folderSyncResponse.status != STATUS_OK) {
                throw MessagingException("Couldn't sync folderlist")
            }

            val newSyncKey = folderSyncResponse.syncKey
            backendStorage.setExtraString(EXTRA_FOLDER_SYNC_KEY, newSyncKey)

            val folderChanges = folderSyncResponse.folderChanges
            if (folderChanges?.folderAdd != null) {
                backendStorage.createFolders(
                        folderChanges.folderAdd.mapNotNull {
                            it.getEmailFolderType()?.let { type ->
                                FolderInfo(it.serverID, it.name, type)
                            }
                        })
            }

            if (folderChanges?.folderDelete != null) {
                backendStorage.deleteFolders(folderChanges.folderDelete)
            }

            folderChanges?.folderUpdate?.forEach {
                it.getEmailFolderType()?.let { type ->
                    backendStorage.changeFolder(it.serverID, it.name, type)

                }
            }
        }
    }

    private fun FolderChange.getEmailFolderType() = when (folderType) {
        FOLDER_TYPE_INBOX -> Folder.FolderType.INBOX
        FOLDER_TYPE_DRAFS -> Folder.FolderType.DRAFTS
        FOLDER_TYPE_TRASH -> Folder.FolderType.TRASH
        FOLDER_TYPE_SENT -> Folder.FolderType.SENT
        FOLDER_TYPE_OUTBOX -> Folder.FolderType.OUTBOX
        FOLDER_TYPE_GENERIC, FOLDER_TYPE_EMAIL_USER_CREATED, FOLDER_TYPE_UNKNOWN -> Folder.FolderType.REGULAR
        else -> null // Not a folder containing emails
    }
}
