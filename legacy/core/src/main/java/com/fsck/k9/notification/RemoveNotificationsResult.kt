package com.fsck.k9.notification

internal data class RemoveNotificationsResult(
    val notificationData: NotificationData,
    val notificationStoreOperations: List<NotificationStoreOperation>,
    val notificationHolders: List<NotificationHolder>,
    val cancelNotificationIds: List<Int>,
)
