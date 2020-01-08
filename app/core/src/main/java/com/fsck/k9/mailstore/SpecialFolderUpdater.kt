package com.fsck.k9.mailstore

import com.fsck.k9.Account
import com.fsck.k9.Account.SpecialFolderSelection
import com.fsck.k9.Preferences
import com.fsck.k9.mail.FolderClass

/**
 * Updates special folders in [Account] if they are marked as [SpecialFolderSelection.AUTOMATIC] or if they are marked
 * as [SpecialFolderSelection.MANUAL] but have been deleted from the server.
 */
class SpecialFolderUpdater(
    private val preferences: Preferences,
    private val folderRepository: FolderRepository,
    private val specialFolderSelectionStrategy: SpecialFolderSelectionStrategy,
    private val account: Account
) {
    fun updateSpecialFolders() {
        val folders = folderRepository.getRemoteFolders()

        updateInbox(folders)
        updateSpecialFolder(FolderType.ARCHIVE, folders)
        updateSpecialFolder(FolderType.DRAFTS, folders)
        updateSpecialFolder(FolderType.SENT, folders)
        updateSpecialFolder(FolderType.SPAM, folders)
        updateSpecialFolder(FolderType.TRASH, folders)

        saveAccount()
    }

    private fun updateInbox(folders: List<Folder>) {
        val oldInboxServerId = account.inboxFolder
        val newInboxServerId = folders.firstOrNull { it.type == FolderType.INBOX }?.serverId
        if (newInboxServerId == oldInboxServerId) return

        account.inboxFolder = newInboxServerId

        if (oldInboxServerId != null && folders.any { it.serverId == oldInboxServerId }) {
            folderRepository.setIncludeInUnifiedInbox(oldInboxServerId, false)
        }

        if (newInboxServerId != null) {
            folderRepository.setIncludeInUnifiedInbox(newInboxServerId, true)
            folderRepository.setDisplayClass(newInboxServerId, FolderClass.FIRST_CLASS)
            folderRepository.setSyncClass(newInboxServerId, FolderClass.FIRST_CLASS)
            folderRepository.setNotificationClass(newInboxServerId, FolderClass.FIRST_CLASS)
        }
    }

    private fun updateSpecialFolder(type: FolderType, folders: List<Folder>) {
        when (getSpecialFolderSelection(type)) {
            SpecialFolderSelection.AUTOMATIC -> {
                val specialFolder = specialFolderSelectionStrategy.selectSpecialFolder(folders, type)
                setSpecialFolder(type, specialFolder?.serverId, SpecialFolderSelection.AUTOMATIC)
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
        if (getSpecialFolder(type) == folder) return

        when (type) {
            FolderType.ARCHIVE -> account.setArchiveFolder(folder, selection)
            FolderType.DRAFTS -> account.setDraftsFolder(folder, selection)
            FolderType.SENT -> account.setSentFolder(folder, selection)
            FolderType.SPAM -> account.setSpamFolder(folder, selection)
            FolderType.TRASH -> account.setTrashFolder(folder, selection)
            else -> throw AssertionError("Unsupported: $type")
        }

        if (folder != null) {
            folderRepository.setDisplayClass(folder, FolderClass.FIRST_CLASS)
            folderRepository.setSyncClass(folder, FolderClass.NO_CLASS)
        }
    }

    private fun saveAccount() {
        preferences.saveAccount(account)
    }
}
