package com.fsck.k9.notification

internal class RemoveNotificationResult private constructor(
    val notificationData: NotificationData,
    val notificationStoreOperations: List<NotificationStoreOperation>,
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
        fun cancelNotification(
            notificationData: NotificationData,
            notificationStoreOperations: List<NotificationStoreOperation>,
            notificationId: Int
        ): RemoveNotificationResult {
            return RemoveNotificationResult(
                notificationData = notificationData,
                notificationStoreOperations = notificationStoreOperations,
                holder = null,
                notificationId = notificationId
            )
        }

        fun replaceNotification(
            notificationData: NotificationData,
            notificationStoreOperations: List<NotificationStoreOperation>,
            notificationHolder: NotificationHolder
        ): RemoveNotificationResult {
            return RemoveNotificationResult(
                notificationData = notificationData,
                notificationStoreOperations = notificationStoreOperations,
                holder = notificationHolder,
                notificationId = notificationHolder.notificationId
            )
        }

        fun recreateSummaryNotification(
            notificationData: NotificationData,
            notificationStoreOperations: List<NotificationStoreOperation>
        ): RemoveNotificationResult {
            return RemoveNotificationResult(
                notificationData = notificationData,
                notificationStoreOperations = notificationStoreOperations,
                holder = null,
                notificationId = null
            )
        }
    }
}
