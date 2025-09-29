package com.fsck.k9.activity

import app.k9mail.legacy.ui.folder.FolderNameFormatter
import com.fsck.k9.mailstore.LocalFolder
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager

class FolderInfoHolder(
    private val folderNameFormatter: FolderNameFormatter,
    private val outboxFolderManager: OutboxFolderManager,
    localFolder: LocalFolder,
    account: LegacyAccount,
) {
    @JvmField
    val databaseId = localFolder.databaseId

    @JvmField
    val displayName = getDisplayName(account, localFolder)

    @JvmField
    var loading = false

    @JvmField
    var moreMessages = localFolder.hasMoreMessages()

    private fun getDisplayName(account: LegacyAccount, localFolder: LocalFolder): String {
        val folderId = localFolder.databaseId
        val folder = Folder(
            id = folderId,
            name = localFolder.name,
            type = getFolderType(outboxFolderManager, account, folderId),
            isLocalOnly = localFolder.isLocalOnly,
        )
        return folderNameFormatter.displayName(folder)
    }

    companion object {
        @JvmStatic
        fun getFolderType(
            outboxFolderManager: OutboxFolderManager,
            account: LegacyAccount,
            folderId: Long,
        ): FolderType {
            return when (folderId) {
                account.inboxFolderId -> FolderType.INBOX
                outboxFolderManager.getOutboxFolderIdSync(account.id) -> FolderType.OUTBOX
                account.archiveFolderId -> FolderType.ARCHIVE
                account.draftsFolderId -> FolderType.DRAFTS
                account.sentFolderId -> FolderType.SENT
                account.spamFolderId -> FolderType.SPAM
                account.trashFolderId -> FolderType.TRASH
                else -> FolderType.REGULAR
            }
        }
    }
}
