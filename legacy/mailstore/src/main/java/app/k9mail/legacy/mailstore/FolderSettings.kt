package app.k9mail.legacy.mailstore

import com.fsck.k9.mail.FolderClass

data class FolderSettings(
    val visibleLimit: Int,
    val displayClass: FolderClass,
    val syncClass: FolderClass,
    val isNotificationsEnabled: Boolean,
    val isPushEnabled: Boolean,
    val inTopGroup: Boolean,
    val integrate: Boolean,
)
