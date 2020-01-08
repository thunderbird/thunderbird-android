package com.fsck.k9.mailstore

import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.FolderType

/**
 * Update special folders when folders are added, removed, or changed.
 */
class SpecialFolderBackendStorageListener(
    private val specialFolderUpdater: SpecialFolderUpdater
) : BackendStorageListener {

    override fun onFoldersCreated(folders: List<FolderInfo>) {
        if (folders.any { it.type != FolderType.REGULAR }) {
            specialFolderUpdater.updateSpecialFolders()
        }
    }

    override fun onFoldersDeleted(folderServerIds: List<String>) {
        specialFolderUpdater.updateSpecialFolders()
    }

    override fun onFolderChanged(folderServerId: String, name: String, type: FolderType) {
        specialFolderUpdater.updateSpecialFolders()
    }
}
