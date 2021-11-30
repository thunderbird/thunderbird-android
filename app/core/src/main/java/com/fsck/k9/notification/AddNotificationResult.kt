package com.fsck.k9.notification

internal class AddNotificationResult private constructor(
    val notificationHolder: NotificationHolder,
    val shouldCancelNotification: Boolean
) {
    val notificationId: Int
        get() {
            check(shouldCancelNotification) { "shouldCancelNotification == false" }
            return notificationHolder.notificationId
        }

    companion object {
        fun newNotification(notificationHolder: NotificationHolder): AddNotificationResult {
            return AddNotificationResult(notificationHolder, shouldCancelNotification = false)
        }

        fun replaceNotification(notificationHolder: NotificationHolder): AddNotificationResult {
            return AddNotificationResult(notificationHolder, shouldCancelNotification = true)
        }
    }
}
