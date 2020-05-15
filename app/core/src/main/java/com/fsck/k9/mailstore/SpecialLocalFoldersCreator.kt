package com.fsck.k9.mailstore

import com.fsck.k9.Account
import com.fsck.k9.Account.SpecialFolderSelection
import com.fsck.k9.Preferences
import com.fsck.k9.mail.FolderType

class SpecialLocalFoldersCreator(
    private val preferences: Preferences,
    private val localStoreProvider: LocalStoreProvider
) {
    fun createSpecialLocalFolders(account: Account) {
        check(account.outboxFolderId == null) { "Outbox folder was already set up" }

        val localStore = localStoreProvider.getInstance(account)

        account.outboxFolderId = localStore.createLocalFolder(OUTBOX_FOLDER_NAME, FolderType.OUTBOX)

        if (account.isPop3()) {
            check(account.draftsFolderId == null) { "Drafts folder was already set up" }
            check(account.sentFolderId == null) { "Sent folder was already set up" }
            check(account.trashFolderId == null) { "Trash folder was already set up" }

            val draftsFolderId = localStore.createLocalFolder(DRAFTS_FOLDER_NAME, FolderType.DRAFTS)
            account.setDraftsFolderId(draftsFolderId, SpecialFolderSelection.MANUAL)

            val sentFolderId = localStore.createLocalFolder(SENT_FOLDER_NAME, FolderType.SENT)
            account.setSentFolderId(sentFolderId, SpecialFolderSelection.MANUAL)

            val trashFolderId = localStore.createLocalFolder(TRASH_FOLDER_NAME, FolderType.TRASH)
            account.setTrashFolderId(trashFolderId, SpecialFolderSelection.MANUAL)
        }

        preferences.saveAccount(account)
    }

    private fun Account.isPop3() = storeUri.startsWith("pop3")

    companion object {
        private const val OUTBOX_FOLDER_NAME = Account.OUTBOX_NAME
        private const val DRAFTS_FOLDER_NAME = "Drafts"
        private const val SENT_FOLDER_NAME = "Sent"
        private const val TRASH_FOLDER_NAME = "Trash"
    }
}
