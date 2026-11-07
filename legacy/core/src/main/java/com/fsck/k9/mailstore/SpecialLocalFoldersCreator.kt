package com.fsck.k9.mailstore

import com.fsck.k9.mail.FolderType
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.AccountId
import net.thunderbird.legacy.logging.Log
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection

private const val TAG = "SpecialLocalFoldersCreator"

class SpecialLocalFoldersCreator(
    private val accountManager: LegacyAccountManager,
    private val localStoreProvider: LocalStoreProvider,
    private val outboxFolderManager: OutboxFolderManager,
) {
    // TODO: When rewriting the account setup code make sure this method is only called once. Until then this can be
    //  called multiple times and we have to make sure folders are only created once.
    suspend fun createSpecialLocalFolders(accountId: AccountId) {
        Log.d(TAG, "Creating special local folders")

        var account = accountManager.findById(accountId) ?: return
        val localStore = localStoreProvider.getInstance(accountId)

        outboxFolderManager.getOutboxFolderId(accountId = accountId, createIfMissing = true)

        if (account.isPop3()) {
            if (account.draftsFolderId == null) {
                val draftsFolderId = localStore.createLocalFolder(DRAFTS_FOLDER_NAME, FolderType.DRAFTS)
                account = account.copy(
                    draftsFolderId = draftsFolderId,
                    draftsFolderSelection = SpecialFolderSelection.MANUAL,
                )
            } else {
                Log.d(TAG, "Drafts folder was already set up")
            }

            if (account.sentFolderId == null) {
                val sentFolderId = localStore.createLocalFolder(SENT_FOLDER_NAME, FolderType.SENT)
                account = account.copy(
                    sentFolderId = sentFolderId,
                    sentFolderSelection = SpecialFolderSelection.MANUAL,
                )
            } else {
                Log.d(TAG, "Sent folder was already set up")
            }

            if (account.trashFolderId == null) {
                val trashFolderId = localStore.createLocalFolder(TRASH_FOLDER_NAME, FolderType.TRASH)
                account = account.copy(
                    trashFolderId = trashFolderId,
                    trashFolderSelection = SpecialFolderSelection.MANUAL,
                )
            } else {
                Log.d(TAG, "Trash folder was already set up")
            }

            saveLocalSpecialFolders(account)
        }
    }

    /**
     * Saves the local special folders of [account] onto the latest version of the account.
     *
     * [account] was read before creating the folders, so saving it as a whole could undo changes made in the meantime.
     */
    private fun saveLocalSpecialFolders(account: LegacyAccount) {
        accountManager.updateSync(account.id) { latestAccount ->
            latestAccount.copy(
                draftsFolderId = account.draftsFolderId,
                draftsFolderSelection = account.draftsFolderSelection,
                sentFolderId = account.sentFolderId,
                sentFolderSelection = account.sentFolderSelection,
                trashFolderId = account.trashFolderId,
                trashFolderSelection = account.trashFolderSelection,
            )
        }
    }

    private fun LegacyAccount.isPop3() = incomingServerSettings.type == Protocols.POP3

    companion object {
        private const val DRAFTS_FOLDER_NAME = "Drafts"
        private const val SENT_FOLDER_NAME = "Sent"
        private const val TRASH_FOLDER_NAME = "Trash"
    }
}
