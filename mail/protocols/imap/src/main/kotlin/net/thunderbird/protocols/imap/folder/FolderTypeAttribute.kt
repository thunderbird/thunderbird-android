package net.thunderbird.protocols.imap.folder

import com.fsck.k9.mail.FolderType

/**
 * **See Also:**
 * [RFC-6154: New Mailbox Attributes Identifying Special-Use Mailboxes](https://www.rfc-editor.org/rfc/rfc6154.html#section-2)
 */
val FolderType.attributeName: String
    get() = when (this) {
        FolderType.DRAFTS -> "\\Drafts"
        FolderType.SENT -> "\\Sent"
        FolderType.TRASH -> "\\Trash"
        FolderType.SPAM -> "\\Junk"
        FolderType.ARCHIVE -> "\\Archive"
        else -> error("FolderType.$this doesn't have an attribute name")
    }
