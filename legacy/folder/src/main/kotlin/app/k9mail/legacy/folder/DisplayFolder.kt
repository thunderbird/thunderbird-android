package app.k9mail.legacy.folder

data class DisplayFolder(
    val folder: Folder,
    val isInTopGroup: Boolean,
    val unreadMessageCount: Int,
    val starredMessageCount: Int,
)
