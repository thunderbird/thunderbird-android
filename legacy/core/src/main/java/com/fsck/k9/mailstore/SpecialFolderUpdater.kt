package com.fsck.k9.mailstore

import app.k9mail.core.common.mail.Protocols
import app.k9mail.legacy.folder.FolderType
import app.k9mail.legacy.folder.RemoteFolder
import com.fsck.k9.Account
import com.fsck.k9.Account.SpecialFolderSelection
import com.fsck.k9.Preferences
import com.fsck.k9.mail.FolderClass

/**
 * Updates special folders in [Account] if they are marked as [SpecialFolderSelection.AUTOMATIC] or if they are marked
 * as [SpecialFolderSelection.MANUAL] but have been deleted from the server.
 */
// TODO: Find a better way to deal with local-only special folders
class SpecialFolderUpdater(
    private val preferences: Preferences,
    private val folderRepository: FolderRepository,
    private val specialFolderSelectionStrategy: SpecialFolderSelectionStrategy,
    private val account: Account,
) {
    fun updateSpecialFolders() {
        val folders = folderRepository.getRemoteFolders(account)

        updateInbox(folders)

        if (!account.isPop3()) {
            updateSpecialFolder(FolderType.ARCHIVE, folders)
            updateSpecialFolder(FolderType.DRAFTS, folders)
            updateSpecialFolder(FolderType.SENT, folders)
            updateSpecialFolder(FolderType.SPAM, folders)
            updateSpecialFolder(FolderType.TRASH, folders)
        }

        removeImportedSpecialFoldersData()
        saveAccount()
    }

    private fun updateInbox(folders: List<RemoteFolder>) {
        val oldInboxId = account.inboxFolderId
        val newInboxId = folders.firstOrNull { it.type == FolderType.INBOX }?.id
        if (newInboxId == oldInboxId) return

        account.inboxFolderId = newInboxId

        if (oldInboxId != null && folders.any { it.id == oldInboxId }) {
            folderRepository.setIncludeInUnifiedInbox(account, oldInboxId, false)
        }

        if (newInboxId != null) {
            folderRepository.setIncludeInUnifiedInbox(account, newInboxId, true)
            folderRepository.setDisplayClass(account, newInboxId, FolderClass.FIRST_CLASS)
            folderRepository.setSyncClass(account, newInboxId, FolderClass.FIRST_CLASS)
            folderRepository.setPushClass(account, newInboxId, FolderClass.FIRST_CLASS)
            folderRepository.setNotificationClass(account, newInboxId, FolderClass.FIRST_CLASS)
        }
    }

    private fun updateSpecialFolder(type: FolderType, folders: List<RemoteFolder>) {
        val importedServerId = getImportedSpecialFolderServerId(type)
        if (importedServerId != null) {
            val folderId = folders.firstOrNull { it.serverId == importedServerId }?.id
            if (folderId != null) {
                setSpecialFolder(type, folderId, getSpecialFolderSelection(type))
                return
            }
        }

        when (getSpecialFolderSelection(type)) {
            SpecialFolderSelection.AUTOMATIC -> {
                val specialFolder = specialFolderSelectionStrategy.selectSpecialFolder(folders, type)
                setSpecialFolder(type, specialFolder?.id, SpecialFolderSelection.AUTOMATIC)
            }
            SpecialFolderSelection.MANUAL -> {
                if (folders.none { it.id == getSpecialFolderId(type) }) {
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

    private fun getSpecialFolderId(type: FolderType): Long? = when (type) {
        FolderType.ARCHIVE -> account.archiveFolderId
        FolderType.DRAFTS -> account.draftsFolderId
        FolderType.SENT -> account.sentFolderId
        FolderType.SPAM -> account.spamFolderId
        FolderType.TRASH -> account.trashFolderId
        else -> throw AssertionError("Unsupported: $type")
    }

    private fun getImportedSpecialFolderServerId(type: FolderType): String? = when (type) {
        FolderType.ARCHIVE -> account.importedArchiveFolder
        FolderType.DRAFTS -> account.importedDraftsFolder
        FolderType.SENT -> account.importedSentFolder
        FolderType.SPAM -> account.importedSpamFolder
        FolderType.TRASH -> account.importedTrashFolder
        else -> throw AssertionError("Unsupported: $type")
    }

    private fun setSpecialFolder(type: FolderType, folderId: Long?, selection: SpecialFolderSelection) {
        if (getSpecialFolderId(type) == folderId) return

        when (type) {
            FolderType.ARCHIVE -> account.setArchiveFolderId(folderId, selection)
            FolderType.DRAFTS -> account.setDraftsFolderId(folderId, selection)
            FolderType.SENT -> account.setSentFolderId(folderId, selection)
            FolderType.SPAM -> account.setSpamFolderId(folderId, selection)
            FolderType.TRASH -> account.setTrashFolderId(folderId, selection)
            else -> throw AssertionError("Unsupported: $type")
        }

        if (folderId != null) {
            folderRepository.setDisplayClass(account, folderId, FolderClass.FIRST_CLASS)
            folderRepository.setSyncClass(account, folderId, FolderClass.NO_CLASS)
        }
    }

    private fun removeImportedSpecialFoldersData() {
        account.importedArchiveFolder = null
        account.importedDraftsFolder = null
        account.importedSentFolder = null
        account.importedSpamFolder = null
        account.importedTrashFolder = null
    }

    private fun saveAccount() {
        preferences.saveAccount(account)
    }

    private fun Account.isPop3() = incomingServerSettings.type == Protocols.POP3
}
