package com.fsck.k9.notification

internal class RemoveNotificationResult private constructor(
    val notificationData: NotificationData,
    private val holder: NotificationHolder?,
    private val notificationId: Int?
) {
    val shouldCreateNotification: Boolean
        get() = holder != null

    val notificationHolder: NotificationHolder
        get() = holder ?: error("shouldCreateNotification == false")

    val shouldCancelNotification: Boolean
        get() = notificationId != null

    val cancelNotificationId: Int
        get() = notificationId ?: error("shouldCancelNotification == false")

    companion object {
        fun cancelNotification(notificationData: NotificationData, notificationId: Int): RemoveNotificationResult {
            return RemoveNotificationResult(
                notificationData = notificationData,
                holder = null,
                notificationId = notificationId
            )
        }

        fun replaceNotification(
            notificationData: NotificationData,
            notificationHolder: NotificationHolder
        ): RemoveNotificationResult {
            return RemoveNotificationResult(
                notificationData = notificationData,
                holder = notificationHolder,
                notificationId = notificationHolder.notificationId
            )
        }

        fun recreateSummaryNotification(notificationData: NotificationData): RemoveNotificationResult {
            return RemoveNotificationResult(
                notificationData = notificationData,
                holder = null,
                notificationId = null
            )
        }
    }
}
