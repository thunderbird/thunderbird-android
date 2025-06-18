package net.thunderbird.feature.notification.api

import net.thunderbird.feature.notification.api.NotificationGroupKey

// TODO: Properly handle notification groups, adding summary, etc.
data class NotificationGroup(
    val key: NotificationGroupKey,
    val summary: String,
)
