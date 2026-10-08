package com.fsck.k9.mail

import androidx.annotation.Discouraged

@Discouraged(
    message = "Use net.thunderbird.feature.mail.folder.api.FolderType instead",
)
enum class FolderType {
    REGULAR,
    INBOX,
    OUTBOX,
    DRAFTS,
    SENT,
    TRASH,
    SPAM,
    ARCHIVE,
}
