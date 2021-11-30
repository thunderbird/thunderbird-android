package com.fsck.k9.notification

internal class NotificationHolder(
    val notificationId: Int,
    val timestamp: Long,
    val content: NotificationContent
)

internal class InactiveNotificationHolder(
    val timestamp: Long,
    val content: NotificationContent
)
