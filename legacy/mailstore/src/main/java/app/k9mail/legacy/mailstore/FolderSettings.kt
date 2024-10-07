package app.k9mail.legacy.mailstore

data class FolderSettings(
    val visibleLimit: Int,
    val isVisible: Boolean,
    val isSyncEnabled: Boolean,
    val isNotificationsEnabled: Boolean,
    val isPushEnabled: Boolean,
    val inTopGroup: Boolean,
    val integrate: Boolean,
)
