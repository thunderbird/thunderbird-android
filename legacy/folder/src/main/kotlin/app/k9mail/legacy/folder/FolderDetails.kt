package app.k9mail.legacy.folder

import com.fsck.k9.mail.FolderClass

data class FolderDetails(
    val folder: Folder,
    val isInTopGroup: Boolean,
    val isIntegrate: Boolean,
    val syncClass: FolderClass,
    val displayClass: FolderClass,
    val isNotificationsEnabled: Boolean,
    val pushClass: FolderClass,
)
