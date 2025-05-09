package com.fsck.k9.mailstore

import app.k9mail.core.common.mail.Protocols
import app.k9mail.core.mail.folder.api.FolderType
import app.k9mail.legacy.account.LegacyAccount
import app.k9mail.legacy.mailstore.FolderRepository
import com.fsck.k9.Preferences
import net.thunderbird.core.mail.folder.api.SpecialFolderSelection
import net.thunderbird.core.mail.folder.api.SpecialFolderUpdater
import net.thunderbird.feature.folder.api.RemoteFolder

/**
 * Updates special folders in [LegacyAccount] if they are marked as [SpecialFolderSelection.AUTOMATIC] or if they
 * are marked as [SpecialFolderSelection.MANUAL] but have been deleted from the server.
 */
// TODO: Find a better way to deal with local-only special folders
class DefaultSpecialFolderUpdater private constructor(
    private val preferences: Preferences,
    private val folderRepository: FolderRepository,
    private val specialFolderSelectionStrategy: SpecialFolderSelectionStrategy,
    private val account: LegacyAccount,
) : SpecialFolderUpdater {
    override fun updateSpecialFolders() {
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
            folderRepository.setVisible(account, newInboxId, true)
            folderRepository.setSyncEnabled(account, newInboxId, true)
            folderRepository.setNotificationsEnabled(account, newInboxId, true)
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
            folderRepository.setVisible(account, folderId, true)
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

    private fun LegacyAccount.isPop3() = incomingServerSettings.type == Protocols.POP3

    class Factory(
        private val preferences: Preferences,
        private val folderRepository: FolderRepository,
        private val specialFolderSelectionStrategy: SpecialFolderSelectionStrategy,
    ) : SpecialFolderUpdater.Factory<LegacyAccount> {
        override fun create(account: LegacyAccount): SpecialFolderUpdater = DefaultSpecialFolderUpdater(
            preferences = preferences,
            folderRepository = folderRepository,
            specialFolderSelectionStrategy = specialFolderSelectionStrategy,
            account = account,
        )
    }
}
