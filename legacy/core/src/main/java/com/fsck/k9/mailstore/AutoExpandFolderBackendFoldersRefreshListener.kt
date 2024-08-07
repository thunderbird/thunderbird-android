package com.fsck.k9.mailstore

import app.k9mail.legacy.account.Account
import app.k9mail.legacy.mailstore.FolderRepository
import com.fsck.k9.Preferences

/**
 * Update an Account's auto-expand folder after the folder list has been refreshed.
 */
class AutoExpandFolderBackendFoldersRefreshListener(
    private val preferences: Preferences,
    private val account: Account,
    private val folderRepository: FolderRepository,
) : BackendFoldersRefreshListener {
    private var isFirstSync = false

    override fun onBeforeFolderListRefresh() {
        isFirstSync = account.inboxFolderId == null
    }

    override fun onAfterFolderListRefresh() {
        checkAutoExpandFolder()

        removeImportedAutoExpandFolder()
        saveAccount()
    }

    private fun checkAutoExpandFolder() {
        account.importedAutoExpandFolder?.let { folderName ->
            if (folderName.isEmpty()) {
                account.autoExpandFolderId = null
            } else {
                val folderId = folderRepository.getFolderId(account, folderName)
                account.autoExpandFolderId = folderId
            }
            return
        }

        account.autoExpandFolderId?.let { autoExpandFolderId ->
            if (!folderRepository.isFolderPresent(account, autoExpandFolderId)) {
                account.autoExpandFolderId = null
            }
        }

        if (isFirstSync && account.autoExpandFolderId == null) {
            account.autoExpandFolderId = account.inboxFolderId
        }
    }

    private fun removeImportedAutoExpandFolder() {
        account.importedAutoExpandFolder = null
    }

    private fun saveAccount() {
        preferences.saveAccount(account)
    }
}
