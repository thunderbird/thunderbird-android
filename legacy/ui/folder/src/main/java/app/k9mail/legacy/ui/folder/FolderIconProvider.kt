package app.k9mail.legacy.ui.folder

import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons
import app.k9mail.legacy.folder.FolderType

class FolderIconProvider {
    fun getFolderIcon(type: FolderType): Int = when (type) {
        FolderType.INBOX -> Icons.Outlined.Inbox
        FolderType.OUTBOX -> Icons.Outlined.Outbox
        FolderType.SENT -> Icons.Outlined.Send
        FolderType.TRASH -> Icons.Outlined.Delete
        FolderType.DRAFTS -> Icons.Outlined.Draft
        FolderType.ARCHIVE -> Icons.Outlined.Archive
        FolderType.SPAM -> Icons.Outlined.Report
        FolderType.REGULAR -> Icons.Outlined.Folder
    }
}
