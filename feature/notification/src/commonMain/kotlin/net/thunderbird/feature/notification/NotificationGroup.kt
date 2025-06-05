package net.thunderbird.feature.notification

// TODO: Properly handle notification groups, adding summary, etc.
data class NotificationGroup(
    val key: NotificationGroupKey,
    val summary: String,
)
