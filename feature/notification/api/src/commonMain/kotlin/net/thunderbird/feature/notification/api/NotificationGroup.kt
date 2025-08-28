package net.thunderbird.feature.notification.api

// TODO: Properly handle notification groups, adding summary, etc.
data class NotificationGroup(
    val key: NotificationGroupKey,
    val summary: String,
)
