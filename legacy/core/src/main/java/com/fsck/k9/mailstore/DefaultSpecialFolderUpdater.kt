package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.FolderRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater

/**
 * Updates special folders in [LegacyAccountDto] if they are marked as [SpecialFolderSelection.AUTOMATIC] or if they
 * are marked as [SpecialFolderSelection.MANUAL] but have been deleted from the server.
 */
// TODO: Find a better way to deal with local-only special folders
@Suppress("TooManyFunctions")
class DefaultSpecialFolderUpdater private constructor(
    private val accountManager: LegacyAccountManager,
    private val folderRepository: FolderRepository,
    private val specialFolderSelectionStrategy: SpecialFolderSelectionStrategy,
    private val accountId: AccountId,
    private val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SpecialFolderUpdater {
    override fun updateSpecialFolders() {
        coroutineScope.launch(ioDispatcher) {
            var account: LegacyAccount = getAccountById(accountId)
            val folders = folderRepository.getRemoteFolders(accountId)

            account = updateInbox(account, folders)

            if (!account.isPop3()) {
                updateSpecialFolder(account, FolderType.ARCHIVE, folders)
                updateSpecialFolder(account, FolderType.DRAFTS, folders)
                updateSpecialFolder(account, FolderType.SENT, folders)
                updateSpecialFolder(account, FolderType.SPAM, folders)
                updateSpecialFolder(account, FolderType.TRASH, folders)
            }

            account = removeImportedSpecialFoldersData(account)

            updateAccount(account)
        }
    }

    private fun updateInbox(account: LegacyAccount, folders: List<RemoteFolder>): LegacyAccount {
        val oldInboxId = account.inboxFolderId
        val newInboxId = folders.firstOrNull { it.type == FolderType.INBOX }?.id
        if (newInboxId == oldInboxId) return account

        val updated = account.copy(inboxFolderId = newInboxId)

        if (oldInboxId != null && folders.any { it.id == oldInboxId }) {
            folderRepository.setIncludeInUnifiedInbox(accountId, oldInboxId, false)
        }

        if (newInboxId != null) {
            folderRepository.setIncludeInUnifiedInbox(accountId, newInboxId, true)
            folderRepository.setVisible(accountId, newInboxId, true)
            folderRepository.setSyncEnabled(accountId, newInboxId, true)
            folderRepository.setNotificationsEnabled(accountId, newInboxId, true)
        }

        return updated
    }

    private fun updateSpecialFolder(account: LegacyAccount, type: FolderType, folders: List<RemoteFolder>) {
        val importedServerId = getImportedSpecialFolderServerId(account, type)
        if (importedServerId != null) {
            val folderId = folders.firstOrNull { it.serverId == importedServerId }?.id
            if (folderId != null) {
                setSpecialFolder(type, folderId, getSpecialFolderSelection(account, type))
                return
            }
        }

        when (getSpecialFolderSelection(account, type)) {
            SpecialFolderSelection.AUTOMATIC -> {
                val specialFolder = specialFolderSelectionStrategy.selectSpecialFolder(folders, type)
                setSpecialFolder(type, specialFolder?.id, SpecialFolderSelection.AUTOMATIC)
            }

            SpecialFolderSelection.MANUAL -> {
                if (folders.none { it.id == getSpecialFolderId(account, type) }) {
                    setSpecialFolder(type, null, SpecialFolderSelection.MANUAL)
                }
            }
        }
    }

    private fun getSpecialFolderSelection(account: LegacyAccount, type: FolderType) = when (type) {
        FolderType.ARCHIVE -> account.copy().archiveFolderSelection
        FolderType.DRAFTS -> account.draftsFolderSelection
        FolderType.SENT -> account.sentFolderSelection
        FolderType.SPAM -> account.spamFolderSelection
        FolderType.TRASH -> account.trashFolderSelection
        else -> throw AssertionError("Unsupported: $type")
    }

    private fun getSpecialFolderId(account: LegacyAccount, type: FolderType): Long? = when (type) {
        FolderType.ARCHIVE -> account.archiveFolderId
        FolderType.DRAFTS -> account.draftsFolderId
        FolderType.SENT -> account.sentFolderId
        FolderType.SPAM -> account.spamFolderId
        FolderType.TRASH -> account.trashFolderId
        else -> throw AssertionError("Unsupported: $type")
    }

    private fun getImportedSpecialFolderServerId(account: LegacyAccount, type: FolderType): String? = when (type) {
        FolderType.ARCHIVE -> account.importedArchiveFolder
        FolderType.DRAFTS -> account.importedDraftsFolder
        FolderType.SENT -> account.importedSentFolder
        FolderType.SPAM -> account.importedSpamFolder
        FolderType.TRASH -> account.importedTrashFolder
        else -> throw AssertionError("Unsupported: $type")
    }

    override fun setSpecialFolder(type: FolderType, folderId: Long?, selection: SpecialFolderSelection) {
        coroutineScope.launch(ioDispatcher) {
            var account = getAccountById(accountId)
            if (getSpecialFolderId(account, type) == folderId) return@launch

            account = when (type) {
                FolderType.ARCHIVE -> {
                    account.copy(
                        archiveFolderId = folderId,
                        archiveFolderSelection = selection,
                    )
                }

                FolderType.DRAFTS -> {
                    account.copy(
                        draftsFolderId = folderId,
                        draftsFolderSelection = selection,
                    )
                }

                FolderType.SENT -> {
                    account.copy(
                        sentFolderId = folderId,
                        sentFolderSelection = selection,
                    )
                }

                FolderType.SPAM -> {
                    account.copy(
                        spamFolderId = folderId,
                        spamFolderSelection = selection,
                    )
                }

                FolderType.TRASH -> {
                    account.copy(
                        trashFolderId = folderId,
                        trashFolderSelection = selection,
                    )
                }

                else -> throw AssertionError("Unsupported: $type")
            }

            updateAccount(account)

            if (folderId != null) {
                folderRepository.setVisible(accountId, folderId, true)
            }
        }
    }

    private fun removeImportedSpecialFoldersData(account: LegacyAccount) = account.copy(
        importedArchiveFolder = null,
        importedDraftsFolder = null,
        importedSentFolder = null,
        importedSpamFolder = null,
        importedTrashFolder = null,
    )

    private suspend fun getAccountById(accountId: AccountId): LegacyAccount {
        return accountManager.getById(accountId).firstOrNull() ?: error("Account not found: $accountId")
    }

    private suspend fun updateAccount(account: LegacyAccount) {
        accountManager.update(account)
    }

    private fun LegacyAccount.isPop3() = incomingServerSettings.type == Protocols.POP3

    class Factory(
        private val accountManager: LegacyAccountManager,
        private val folderRepository: FolderRepository,
        private val specialFolderSelectionStrategy: SpecialFolderSelectionStrategy,
        private val coroutineScope: CoroutineScope,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : SpecialFolderUpdater.Factory {
        override fun create(accountId: AccountId): SpecialFolderUpdater = DefaultSpecialFolderUpdater(
            accountManager = accountManager,
            folderRepository = folderRepository,
            specialFolderSelectionStrategy = specialFolderSelectionStrategy,
            accountId = accountId,
            coroutineScope = coroutineScope,
            ioDispatcher = ioDispatcher,
        )
    }
}
