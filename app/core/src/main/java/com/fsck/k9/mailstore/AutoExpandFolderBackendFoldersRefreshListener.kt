package com.fsck.k9.mailstore

import com.fsck.k9.Account
import com.fsck.k9.Preferences

/**
 * Update an Account's auto-expand folder after the folder list has been refreshed.
 */
class AutoExpandFolderBackendFoldersRefreshListener(
    private val preferences: Preferences,
    private val account: Account,
    private val folderRepository: FolderRepository
) : BackendFoldersRefreshListener {

    override fun onBeforeFolderListRefresh() = Unit

    override fun onAfterFolderListRefresh() {
        checkAutoExpandFolder()

        removeImportedAutoExpandFolder()
        saveAccount()
    }

    private fun checkAutoExpandFolder() {
        val folderId = account.importedAutoExpandFolder?.let { folderRepository.getFolderId(it) }
        if (folderId != null) {
            account.autoExpandFolderId = folderId
            return
        }

        account.autoExpandFolderId?.let { autoExpandFolderId ->
            if (!folderRepository.isFolderPresent(autoExpandFolderId)) {
                account.autoExpandFolderId = null
            }
        }
    }

    private fun removeImportedAutoExpandFolder() {
        account.importedAutoExpandFolder = null
    }

    private fun saveAccount() {
        preferences.saveAccount(account)
    }
}
