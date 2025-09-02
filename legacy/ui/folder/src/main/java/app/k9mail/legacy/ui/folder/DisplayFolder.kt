package app.k9mail.legacy.ui.folder

import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter

data class DisplayFolder(
    val folder: Folder,
    val isInTopGroup: Boolean,
    val unreadMessageCount: Int,
    val starredMessageCount: Int,
    val pathDelimiter: FolderPathDelimiter,
)
