package com.fsck.k9.mailstore

import com.fsck.k9.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.mail.Folder.FolderClass

class FolderRepository(private val account: Account) {
    private val sortForDisplay = compareByDescending<LocalFolder> { it.serverId == account.inboxFolder }
            .thenByDescending { it.serverId == account.outboxFolder }
            .thenByDescending { it.isInTopGroup }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }


    fun getRemoteFolders(): List<Folder> {
        val folders = account.localStore.getPersonalNamespaces(false)
        val outbox = account.outboxFolder

        return folders
                .filter { it.serverId != outbox }
                .map(::createFolderFromLocalFolder)
    }

    fun getDisplayFolders(): List<Folder> {
        val folders = account.localStore.getPersonalNamespaces(false)
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
}

data class Folder(val id: Long, val serverId: String, val name: String, val type: FolderType)

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
