package com.fsck.k9.mailstore

import com.fsck.k9.Account

class FolderRepository(private val account: Account) {

    fun getRemoteFolders(): List<Folder> {
        val folders = account.localStore.getPersonalNamespaces(false)
        val outbox = account.outboxFolder

        return folders
                .filter { it.serverId != outbox }
                .map { Folder(it.databaseId, it.serverId, it.name, folderTypeOf(it)) }
    }

    private fun folderTypeOf(folder: LocalFolder) = when (folder.serverId) {
        account.inboxFolder -> FolderType.INBOX
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
    SENT,
    TRASH,
    DRAFTS,
    ARCHIVE,
    SPAM
}
