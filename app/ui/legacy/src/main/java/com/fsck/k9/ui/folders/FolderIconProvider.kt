package com.fsck.k9.ui.folders

import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons
import com.fsck.k9.mailstore.FolderType

class FolderIconProvider {
    private val iconFolderInboxResId: Int = Icons.Outlined.Inbox
    private val iconFolderOutboxResId: Int = Icons.Outlined.Outbox
    private val iconFolderSentResId: Int = Icons.Filled.Send
    private val iconFolderTrashResId: Int = Icons.Filled.Trash
    private val iconFolderDraftsResId: Int = Icons.Filled.Drafts
    private val iconFolderArchiveResId: Int = Icons.Filled.Archive
    private val iconFolderSpamResId: Int = Icons.Filled.Spam
    var iconFolderResId: Int = Icons.Filled.Folder

    fun getFolderIcon(type: FolderType): Int = when (type) {
        FolderType.INBOX -> iconFolderInboxResId
        FolderType.OUTBOX -> iconFolderOutboxResId
        FolderType.SENT -> iconFolderSentResId
        FolderType.TRASH -> iconFolderTrashResId
        FolderType.DRAFTS -> iconFolderDraftsResId
        FolderType.ARCHIVE -> iconFolderArchiveResId
        FolderType.SPAM -> iconFolderSpamResId
        else -> iconFolderResId
    }
}
