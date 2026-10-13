package net.thunderbird.feature.mail.folder

/**
 * The role of a special folder in a mail account, such as the inbox or the sent folder.
 *
 * A mail account has at most one folder per role. Regular folders have no role.
 */
public enum class FolderRole {
    INBOX,
    OUTBOX,
    DRAFTS,
    SENT,
    ARCHIVE,
    SPAM,
    TRASH,
}
