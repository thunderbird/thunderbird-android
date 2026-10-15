package com.fsck.k9.ui.messagelist

import androidx.annotation.StringRes
import com.fsck.k9.search.unifiedFolderRole
import com.fsck.k9.ui.R
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.feature.search.legacy.LocalMessageSearch

/**
 * The title of a unified search, named after the role of the folders it shows.
 */
@get:StringRes
internal val LocalMessageSearch.unifiedFolderTitleResId: Int
    get() = when (unifiedFolderRole) {
        FolderRole.OUTBOX -> R.string.unified_outbox_title
        FolderRole.DRAFTS -> R.string.unified_drafts_title
        FolderRole.SENT -> R.string.unified_sent_title
        FolderRole.ARCHIVE -> R.string.unified_archive_title
        FolderRole.SPAM -> R.string.unified_spam_title
        FolderRole.TRASH -> R.string.unified_trash_title
        FolderRole.INBOX, null -> R.string.integrated_inbox_title
    }
