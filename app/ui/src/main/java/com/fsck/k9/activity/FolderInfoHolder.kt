package com.fsck.k9.activity

import com.fsck.k9.Account
import com.fsck.k9.mailstore.Folder
import com.fsck.k9.mailstore.FolderType
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.ui.folders.FolderNameFormatter

class FolderInfoHolder(
    private val folderNameFormatter: FolderNameFormatter,
    localFolder: LocalFolder,
    account: Account
) {
    @JvmField val databaseId = localFolder.databaseId
    @JvmField val serverId: String = localFolder.serverId
    @JvmField val displayName = getDisplayName(account, localFolder)
    @JvmField var loading = false
    @JvmField var moreMessages = localFolder.hasMoreMessages()

    private fun getDisplayName(account: Account, localFolder: LocalFolder): String {
        val serverId = localFolder.serverId
        val folder = Folder(
            localFolder.databaseId,
            serverId,
            localFolder.name,
            getFolderType(account, serverId)
        )
        return folderNameFormatter.displayName(folder)
    }

    companion object {
        @JvmStatic
        fun getFolderType(account: Account, serverId: String): FolderType {
            return when (serverId) {
                account.inboxFolder -> FolderType.INBOX
                account.outboxFolder -> FolderType.OUTBOX
                account.archiveFolder -> FolderType.ARCHIVE
                account.draftsFolder -> FolderType.DRAFTS
                account.sentFolder -> FolderType.SENT
                account.spamFolder -> FolderType.SPAM
                account.trashFolder -> FolderType.TRASH
                else -> FolderType.REGULAR
            }
        }
    }
}
