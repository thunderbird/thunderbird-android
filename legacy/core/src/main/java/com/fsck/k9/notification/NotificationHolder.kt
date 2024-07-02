package com.fsck.k9.notification

internal data class NotificationHolder(
    val notificationId: Int,
    val timestamp: Long,
    val content: NotificationContent,
)

internal data class InactiveNotificationHolder(
    val timestamp: Long,
    val content: NotificationContent,
)
