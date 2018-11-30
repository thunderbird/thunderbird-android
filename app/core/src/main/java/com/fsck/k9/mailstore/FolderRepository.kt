package com.fsck.k9.mailstore

import com.fsck.k9.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.mail.Folder.FolderClass
import com.fsck.k9.mail.Folder.FolderType as RemoteFolderType

class FolderRepository(
        private val localStoreProvider: LocalStoreProvider,
        private val specialFolderSelectionStrategy: SpecialFolderSelectionStrategy,
        private val account: Account
) {
    private val sortForDisplay = compareByDescending<LocalFolder> { it.serverId == account.inboxFolder }
            .thenByDescending { it.serverId == account.outboxFolder }
            .thenByDescending { account.isSpecialFolder(it.serverId) }
            .thenByDescending { it.isInTopGroup }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }


    fun getRemoteFolderInfo(): RemoteFolderInfo {
        val folders = getRemoteFolders()
        val automaticSpecialFolders = mapOf(
                FolderType.ARCHIVE to specialFolderSelectionStrategy.selectSpecialFolder(folders, FolderType.ARCHIVE),
                FolderType.DRAFTS to specialFolderSelectionStrategy.selectSpecialFolder(folders, FolderType.DRAFTS),
                FolderType.SENT to specialFolderSelectionStrategy.selectSpecialFolder(folders, FolderType.SENT),
                FolderType.SPAM to specialFolderSelectionStrategy.selectSpecialFolder(folders, FolderType.SPAM),
                FolderType.TRASH to specialFolderSelectionStrategy.selectSpecialFolder(folders, FolderType.TRASH)
        )

        return RemoteFolderInfo(folders, automaticSpecialFolders)
    }

    private fun getRemoteFolders(): List<Folder> {
        val folders = localStoreProvider.getInstance(account).getPersonalNamespaces(false)

        return folders
                .filterNot { it.isLocalOnly }
                .map { Folder(it.databaseId, it.serverId, it.name, it.type.toFolderType()) }
    }

    fun getDisplayFolders(): List<Folder> {
        val folders = localStoreProvider.getInstance(account).getPersonalNamespaces(false)
        return folders
                .filter(::shouldDisplayFolder)
                .sortedWith(sortForDisplay)
                .map(::createFolderFromLocalFolder)
    }

    private fun shouldDisplayFolder(localFolder: LocalFolder): Boolean {
        val displayMode = account.folderDisplayMode
        val displayClass = localFolder.displayClass
        return when (displayMode) {
            FolderMode.ALL -> true
            FolderMode.FIRST_CLASS -> displayClass == FolderClass.FIRST_CLASS
            FolderMode.FIRST_AND_SECOND_CLASS -> {
                displayClass == FolderClass.FIRST_CLASS || displayClass == FolderClass.SECOND_CLASS
            }
            FolderMode.NOT_SECOND_CLASS -> displayClass != FolderClass.SECOND_CLASS
            else -> throw AssertionError("Invalid folder display mode: $displayMode")
        }
    }

    private fun createFolderFromLocalFolder(localFolder: LocalFolder): Folder {
        return Folder(localFolder.databaseId, localFolder.serverId, localFolder.name, folderTypeOf(localFolder))
    }

    private fun folderTypeOf(folder: LocalFolder) = when (folder.serverId) {
        account.inboxFolder -> FolderType.INBOX
        account.outboxFolder -> FolderType.OUTBOX
        account.sentFolder -> FolderType.SENT
        account.trashFolder -> FolderType.TRASH
        account.draftsFolder -> FolderType.DRAFTS
        account.archiveFolder -> FolderType.ARCHIVE
        account.spamFolder -> FolderType.SPAM
        else -> FolderType.REGULAR
    }

    private fun RemoteFolderType.toFolderType(): FolderType = when (this) {
        RemoteFolderType.REGULAR -> FolderType.REGULAR
        RemoteFolderType.INBOX -> FolderType.INBOX
        RemoteFolderType.OUTBOX -> FolderType.REGULAR   // We currently don't support remote Outbox folders
        RemoteFolderType.DRAFTS -> FolderType.DRAFTS
        RemoteFolderType.SENT -> FolderType.SENT
        RemoteFolderType.TRASH -> FolderType.TRASH
        RemoteFolderType.SPAM -> FolderType.SPAM
        RemoteFolderType.ARCHIVE -> FolderType.ARCHIVE
    }
}

data class Folder(val id: Long, val serverId: String, val name: String, val type: FolderType)

data class RemoteFolderInfo(val folders: List<Folder>, val automaticSpecialFolders: Map<FolderType, Folder?>)

enum class FolderType {
    REGULAR,
    INBOX,
    OUTBOX,
    SENT,
    TRASH,
    DRAFTS,
    ARCHIVE,
    SPAM
}
