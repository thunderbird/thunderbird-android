@file:JvmName("FolderTypeConverter")

package com.fsck.k9.mailstore

import com.fsck.k9.mail.FolderType

@JvmName("fromDatabaseFolderType")
fun String.toFolderType(): FolderType {
    return when (this) {
        "regular" -> FolderType.REGULAR
        "inbox" -> FolderType.INBOX
        "outbox" -> FolderType.OUTBOX
        "drafts" -> FolderType.DRAFTS
        "sent" -> FolderType.SENT
        "trash" -> FolderType.TRASH
        "spam" -> FolderType.SPAM
        "archive" -> FolderType.ARCHIVE
        else -> throw AssertionError("Unknown folder type: $this")
    }
}

fun FolderType.toDatabaseFolderType(): String {
    return when (this) {
        FolderType.REGULAR -> "regular"
        FolderType.INBOX -> "inbox"
        FolderType.OUTBOX -> "outbox"
        FolderType.DRAFTS -> "drafts"
        FolderType.SENT -> "sent"
        FolderType.TRASH -> "trash"
        FolderType.SPAM -> "spam"
        FolderType.ARCHIVE -> "archive"
    }
}
