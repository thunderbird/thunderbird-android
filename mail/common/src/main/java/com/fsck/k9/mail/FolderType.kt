package com.fsck.k9.mail

@Deprecated(
    message = "Use net.thunderbird.feature.mail.folder.api.FolderType instead",
    replaceWith = ReplaceWith(
        expression = "FolderType",
        imports = ["net.thunderbird.feature.mail.folder.api.FolderType"],
    ),
    level = DeprecationLevel.WARNING,
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
