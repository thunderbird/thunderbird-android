package app.k9mail.core.mail.folder.api

import com.fsck.k9.mail.FolderClass

data class FolderDetails(
    val folder: Folder,
    val isInTopGroup: Boolean,
    val isIntegrate: Boolean,
    val syncClass: FolderClass,
    val displayClass: FolderClass,
    val isNotificationsEnabled: Boolean,
    val isPushEnabled: Boolean,
)
