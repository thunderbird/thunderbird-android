package com.fsck.k9.mailstore

import com.fsck.k9.Account
import com.fsck.k9.Account.SpecialFolderSelection
import com.fsck.k9.Preferences

/**
 * Updates special folders in [Account] if they are marked as [SpecialFolderSelection.AUTOMATIC] or if they are marked
 * as [SpecialFolderSelection.MANUAL] but have been deleted from the server.
 */
class SpecialFolderUpdater(
        private val preferences: Preferences,
        private val folderRepository: FolderRepository,
        private val account: Account
) {
    fun updateSpecialFolders() {
        val (folders, automaticSpecialFolders) = folderRepository.getRemoteFolderInfo()

        updateInbox(folders)
        updateSpecialFolder(FolderType.ARCHIVE, folders, automaticSpecialFolders)
        updateSpecialFolder(FolderType.DRAFTS, folders, automaticSpecialFolders)
        updateSpecialFolder(FolderType.SENT, folders, automaticSpecialFolders)
        updateSpecialFolder(FolderType.SPAM, folders, automaticSpecialFolders)
        updateSpecialFolder(FolderType.TRASH, folders, automaticSpecialFolders)

        saveAccount()
    }

    private fun updateInbox(folders: List<Folder>) {
        account.inboxFolder = folders.firstOrNull { it.type == FolderType.INBOX }?.serverId
    }

    private fun updateSpecialFolder(
            type: FolderType,
            folders: List<Folder>,
            automaticSpecialFolders: Map<FolderType, Folder?>
    ) {
        when (getSpecialFolderSelection(type)) {
            SpecialFolderSelection.AUTOMATIC -> {
                setSpecialFolder(type, automaticSpecialFolders[type]?.serverId, SpecialFolderSelection.AUTOMATIC)
            }
            SpecialFolderSelection.MANUAL -> {
                if (folders.none { it.serverId == getSpecialFolder(type) }) {
                    setSpecialFolder(type, null, SpecialFolderSelection.MANUAL)
                }
            }
        }
    }

    private fun getSpecialFolderSelection(type: FolderType) = when (type) {
        FolderType.ARCHIVE -> account.archiveFolderSelection
        FolderType.DRAFTS -> account.draftsFolderSelection
        FolderType.SENT -> account.sentFolderSelection
        FolderType.SPAM -> account.spamFolderSelection
        FolderType.TRASH -> account.trashFolderSelection
        else -> throw AssertionError("Unsupported: $type")
    }

    private fun getSpecialFolder(type: FolderType): String? = when (type) {
        FolderType.ARCHIVE -> account.archiveFolder
        FolderType.DRAFTS -> account.draftsFolder
        FolderType.SENT -> account.sentFolder
        FolderType.SPAM -> account.spamFolder
        FolderType.TRASH -> account.trashFolder
        else -> throw AssertionError("Unsupported: $type")
    }

    private fun setSpecialFolder(type: FolderType, folder: String?, selection: SpecialFolderSelection) {
        when (type) {
            FolderType.ARCHIVE -> account.setArchiveFolder(folder, selection)
            FolderType.DRAFTS -> account.setDraftsFolder(folder, selection)
            FolderType.SENT -> account.setSentFolder(folder, selection)
            FolderType.SPAM -> account.setSpamFolder(folder, selection)
            FolderType.TRASH -> account.setTrashFolder(folder, selection)
            else -> throw AssertionError("Unsupported: $type")
        }
    }

    private fun saveAccount() {
        account.save()
    }
}
