package com.fsck.k9.notification

internal class AddNotificationResult private constructor(
    val notificationData: NotificationData,
    val notificationStoreOperations: List<NotificationStoreOperation>,
    val notificationHolder: NotificationHolder,
    val shouldCancelNotification: Boolean,
) {
    val cancelNotificationId: Int
        get() {
            check(shouldCancelNotification) { "shouldCancelNotification == false" }
            return notificationHolder.notificationId
        }

    companion object {
        fun newNotification(
            notificationData: NotificationData,
            notificationStoreOperations: List<NotificationStoreOperation>,
            notificationHolder: NotificationHolder,
        ): AddNotificationResult {
            return AddNotificationResult(
                notificationData,
                notificationStoreOperations,
                notificationHolder,
                shouldCancelNotification = false,
            )
        }

        fun replaceNotification(
            notificationData: NotificationData,
            notificationStoreOperations: List<NotificationStoreOperation>,
            notificationHolder: NotificationHolder,
        ): AddNotificationResult {
            return AddNotificationResult(
                notificationData,
                notificationStoreOperations,
                notificationHolder,
                shouldCancelNotification = true,
            )
        }
    }
}
