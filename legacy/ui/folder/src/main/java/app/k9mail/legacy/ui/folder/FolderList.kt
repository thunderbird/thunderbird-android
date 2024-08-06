package app.k9mail.legacy.ui.folder

import app.k9mail.legacy.folder.DisplayFolder

data class FolderList(
    val unifiedInbox: DisplayUnifiedInbox?,
    val accountId: Int,
    val folders: List<DisplayFolder>,
)
