package app.k9mail.legacy.mailstore

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.mail.folder.api.FolderType

object FolderTypeMapper {

    fun folderTypeOf(account: LegacyAccount, folderId: Long) = when (folderId) {
        account.inboxFolderId -> FolderType.INBOX
        account.sentFolderId -> FolderType.SENT
        account.trashFolderId -> FolderType.TRASH
        account.draftsFolderId -> FolderType.DRAFTS
        account.archiveFolderId -> FolderType.ARCHIVE
        account.spamFolderId -> FolderType.SPAM
        else -> FolderType.REGULAR
    }
}
