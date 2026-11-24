package com.fsck.k9.mailstore

import net.thunderbird.components.core.outcome.fold
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
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

        account = removeImportedAutoExpandFolder(account)

        saveAutoExpandFolder(account)
    }

    private suspend fun checkAutoExpandFolder(account: LegacyAccount): LegacyAccount {
        var updated = account

        updated.importedAutoExpandFolder?.let { folderName ->
            if (folderName.isEmpty()) {
                updated = updated.copy(autoExpandFolderId = null)
            } else {
                val folderId = folderQueryRepository.findIdByServerId(accountId, FolderServerId(folderName))
                    .fold(
                        onSuccess = { it },
                        onFailure = { error ->
                            when (val throwable = error.throwable) {
                                null -> null
                                else -> throw throwable
                            }
                        },
                    )
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
        return accountManager.findById(id)
            ?: error("Account not found with ID: $id")
    }

    /**
     * Saves the auto-expand folder settings of [account] onto the latest version of the account.
     *
     * [account] was read before querying folders, so saving it as a whole could undo changes made in the meantime.
     */
    private fun saveAutoExpandFolder(account: LegacyAccount) {
        accountManager.updateSync(accountId) { latestAccount ->
            latestAccount.copy(
                autoExpandFolderId = account.autoExpandFolderId,
                importedAutoExpandFolder = account.importedAutoExpandFolder,
            )
        }
    }
}
