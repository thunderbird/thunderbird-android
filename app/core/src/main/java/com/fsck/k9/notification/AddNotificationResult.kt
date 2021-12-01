package com.fsck.k9.notification

internal class AddNotificationResult private constructor(
    val notificationData: NotificationData,
    val notificationHolder: NotificationHolder,
    val shouldCancelNotification: Boolean
) {
    val cancelNotificationId: Int
        get() {
            check(shouldCancelNotification) { "shouldCancelNotification == false" }
            return notificationHolder.notificationId
        }

    companion object {
        fun newNotification(
            notificationData: NotificationData,
            notificationHolder: NotificationHolder
        ): AddNotificationResult {
            return AddNotificationResult(notificationData, notificationHolder, shouldCancelNotification = false)
        }

        fun replaceNotification(
            notificationData: NotificationData,
            notificationHolder: NotificationHolder
        ): AddNotificationResult {
            return AddNotificationResult(notificationData, notificationHolder, shouldCancelNotification = true)
        }
    }
}
