package com.fsck.k9.mailstore

import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.FolderType

/**
 * Reset an Account's auto-expand folder when the currently configured folder was removed.
 */
class AutoExpandFolderBackendStorageListener(
    private val preferences: Preferences,
    private val account: Account
) : BackendStorageListener {

    override fun onFoldersCreated(folders: List<FolderInfo>) = Unit

    override fun onFoldersDeleted(folderServerIds: List<String>) {
        if (account.autoExpandFolder in folderServerIds) {
            account.autoExpandFolder = null
            preferences.saveAccount(account)
        }
    }

    override fun onFolderChanged(folderServerId: String, name: String, type: FolderType) = Unit
}
