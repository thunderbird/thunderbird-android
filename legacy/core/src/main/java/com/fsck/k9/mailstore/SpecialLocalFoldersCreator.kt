package com.fsck.k9.mailstore

import com.fsck.k9.Preferences
import com.fsck.k9.mail.FolderType
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection

class SpecialLocalFoldersCreator(
    private val preferences: Preferences,
    private val localStoreProvider: LocalStoreProvider,
    private val outboxFolderManager: OutboxFolderManager,
) {
    // TODO: When rewriting the account setup code make sure this method is only called once. Until then this can be
    //  called multiple times and we have to make sure folders are only created once.
    suspend fun createSpecialLocalFolders(account: LegacyAccountDto) {
        Log.d("Creating special local folders")

        val localStore = localStoreProvider.getInstance(account)

        outboxFolderManager.getOutboxFolderId(accountId = account.id, createIfMissing = true)

        if (account.isPop3()) {
            if (account.draftsFolderId == null) {
                val draftsFolderId = localStore.createLocalFolder(DRAFTS_FOLDER_NAME, FolderType.DRAFTS)
                account.setDraftsFolderId(draftsFolderId, SpecialFolderSelection.MANUAL)
            } else {
                Log.d("Drafts folder was already set up")
            }

            if (account.sentFolderId == null) {
                val sentFolderId = localStore.createLocalFolder(SENT_FOLDER_NAME, FolderType.SENT)
                account.setSentFolderId(sentFolderId, SpecialFolderSelection.MANUAL)
            } else {
                Log.d("Sent folder was already set up")
            }

            if (account.trashFolderId == null) {
                val trashFolderId = localStore.createLocalFolder(TRASH_FOLDER_NAME, FolderType.TRASH)
                account.setTrashFolderId(trashFolderId, SpecialFolderSelection.MANUAL)
            } else {
                Log.d("Trash folder was already set up")
            }
        }

        preferences.saveAccount(account)
    }

    private fun LegacyAccountDto.isPop3() = incomingServerSettings.type == Protocols.POP3

    companion object {
        private const val DRAFTS_FOLDER_NAME = "Drafts"
        private const val SENT_FOLDER_NAME = "Sent"
        private const val TRASH_FOLDER_NAME = "Trash"
    }
}
