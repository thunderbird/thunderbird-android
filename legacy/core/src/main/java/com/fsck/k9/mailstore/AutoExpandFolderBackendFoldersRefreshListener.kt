package com.fsck.k9.mailstore

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.outcome.fold
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.FolderServerId
import net.thunderbird.feature.mail.folder.api.data.repository.FolderQueryRepository

/**
 * Update an Account's auto-expand folder after the folder list has been refreshed.
 */
class AutoExpandFolderBackendFoldersRefreshListener(
    private val accountManager: LegacyAccountManager,
    private val accountId: AccountId,
    private val folderQueryRepository: FolderQueryRepository,
) : BackendFoldersRefreshListener {
    private var isFirstSync = false

    override fun onBeforeFolderListRefresh() {
        isFirstSync = getAccountById(accountId).inboxFolderId == null
    }

    override suspend fun onAfterFolderListRefresh() {
        var account = getAccountById(accountId)

        account = checkAutoExpandFolder(account)

        removeImportedAutoExpandFolder(account)

        updateAccount(account)
    }

    private suspend fun checkAutoExpandFolder(account: LegacyAccount): LegacyAccount {
        var updated = account

        updated.importedAutoExpandFolder?.let { folderName ->
            if (folderName.isEmpty()) {
                updated = updated.copy(autoExpandFolderId = null)
            } else {
                val folderId = folderQueryRepository.findIdByServerId(accountId, FolderServerId(folderName))
                    .fold(onSuccess = { it }, onFailure = { null })
                updated = updated.copy(autoExpandFolderId = folderId)
            }
            return updated
        }

        updated.autoExpandFolderId?.let { autoExpandFolderId ->
            if (!folderQueryRepository.isPresent(accountId, autoExpandFolderId)) {
                updated = updated.copy(autoExpandFolderId = null)
            }
        }

        if (isFirstSync && updated.autoExpandFolderId == null) {
            updated = updated.copy(autoExpandFolderId = updated.inboxFolderId)
        }

        return updated
    }

    private fun removeImportedAutoExpandFolder(account: LegacyAccount): LegacyAccount {
        return account.copy(importedAutoExpandFolder = null)
    }

    private fun getAccountById(id: AccountId): LegacyAccount {
        return accountManager.getByIdSync(id)
            ?: error("Account not found with ID: $id")
    }

    private fun updateAccount(account: LegacyAccount) {
        accountManager.updateSync(account)
    }
}
