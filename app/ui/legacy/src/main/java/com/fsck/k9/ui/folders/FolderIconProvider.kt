package com.fsck.k9.ui.folders

import android.content.res.Resources
import android.util.TypedValue
import com.fsck.k9.mail.FolderType as LegacyFolderType
import com.fsck.k9.mailstore.FolderType
import com.fsck.k9.ui.R

class FolderIconProvider(private val theme: Resources.Theme) {
    private val iconFolderInboxResId: Int
    private val iconFolderOutboxResId: Int
    private val iconFolderSentResId: Int
    private val iconFolderTrashResId: Int
    private val iconFolderDraftsResId: Int
    private val iconFolderArchiveResId: Int
    private val iconFolderSpamResId: Int
    var iconFolderResId: Int

    init {
        iconFolderInboxResId = getResId(R.attr.iconFolderInbox)
        iconFolderOutboxResId = getResId(R.attr.iconFolderOutbox)
        iconFolderSentResId = getResId(R.attr.iconFolderSent)
        iconFolderTrashResId = getResId(R.attr.iconFolderTrash)
        iconFolderDraftsResId = getResId(R.attr.iconFolderDrafts)
        iconFolderArchiveResId = getResId(R.attr.iconFolderArchive)
        iconFolderSpamResId = getResId(R.attr.iconFolderSpam)
        iconFolderResId = getResId(R.attr.iconFolder)
    }

    private fun getResId(resAttribute: Int): Int {
        val typedValue = TypedValue()
        val found = theme.resolveAttribute(resAttribute, typedValue, true)
        if (!found) {
            throw AssertionError("Couldn't find resource with attribute $resAttribute")
        }
        return typedValue.resourceId
    }

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

    fun getFolderIcon(type: LegacyFolderType): Int = when (type) {
        LegacyFolderType.INBOX -> iconFolderInboxResId
        LegacyFolderType.OUTBOX -> iconFolderOutboxResId
        LegacyFolderType.SENT -> iconFolderSentResId
        LegacyFolderType.TRASH -> iconFolderTrashResId
        LegacyFolderType.DRAFTS -> iconFolderDraftsResId
        LegacyFolderType.ARCHIVE -> iconFolderArchiveResId
        LegacyFolderType.SPAM -> iconFolderSpamResId
        else -> iconFolderResId
    }
}
