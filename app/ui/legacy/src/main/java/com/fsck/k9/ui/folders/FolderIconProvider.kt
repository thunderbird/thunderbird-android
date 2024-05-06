package com.fsck.k9.ui.folders

import com.fsck.k9.mailstore.FolderType
import com.fsck.k9.ui.R

class FolderIconProvider {
    private val iconFolderInboxResId: Int = R.drawable.ic_inbox
    private val iconFolderOutboxResId: Int = R.drawable.ic_outbox
    private val iconFolderSentResId: Int = R.drawable.ic_send
    private val iconFolderTrashResId: Int = R.drawable.ic_trash_can
    private val iconFolderDraftsResId: Int = R.drawable.ic_drafts_folder
    private val iconFolderArchiveResId: Int = R.drawable.ic_archive
    private val iconFolderSpamResId: Int = R.drawable.ic_alert_octagon
    var iconFolderResId: Int = R.drawable.ic_folder

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
