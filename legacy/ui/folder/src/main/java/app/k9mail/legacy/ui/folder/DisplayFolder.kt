package app.k9mail.legacy.ui.folder

import app.k9mail.core.mail.folder.api.Folder

data class DisplayFolder(
    val folder: Folder,
    val isInTopGroup: Boolean,
    val unreadMessageCount: Int,
    val starredMessageCount: Int,
)
