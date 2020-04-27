package com.fsck.k9.mailstore

import com.fsck.k9.Account
import com.fsck.k9.Preferences

/**
 * Reset an Account's auto-expand folder when the currently configured folder was removed.
 */
class AutoExpandFolderBackendFoldersRefreshListener(
    private val preferences: Preferences,
    private val account: Account,
    private val folderRepository: FolderRepository
) : BackendFoldersRefreshListener {

    override fun onBeforeFolderListRefresh() = Unit

    override fun onAfterFolderListRefresh() {
        checkAutoExpandFolder()
    }

    private fun checkAutoExpandFolder() {
        account.autoExpandFolder?.let { autoExpandFolderServerId ->
            if (!folderRepository.isFolderPresent(autoExpandFolderServerId)) {
                account.autoExpandFolder = null
                preferences.saveAccount(account)
            }
        }
    }
}
