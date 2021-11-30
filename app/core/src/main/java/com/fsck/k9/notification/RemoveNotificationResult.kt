package com.fsck.k9.notification

internal class RemoveNotificationResult private constructor(
    private val holder: NotificationHolder?,
    notificationId: Int,
    val isUnknownNotification: Boolean
) {
    val notificationId: Int = notificationId
        get() {
            check(!isUnknownNotification) { "isUnknownNotification == true" }
            return field
        }

    val shouldCreateNotification: Boolean
        get() = holder != null

    val notificationHolder: NotificationHolder
        get() = holder ?: error("shouldCreateNotification == false")

    companion object {
        fun createNotification(notificationHolder: NotificationHolder): RemoveNotificationResult {
            return RemoveNotificationResult(
                holder = notificationHolder,
                notificationId = notificationHolder.notificationId,
                isUnknownNotification = false
            )
        }

        fun cancelNotification(notificationId: Int): RemoveNotificationResult {
            return RemoveNotificationResult(
                holder = null,
                notificationId = notificationId,
                isUnknownNotification = false
            )
        }

        fun unknownNotification(): RemoveNotificationResult {
            return RemoveNotificationResult(holder = null, notificationId = 0, isUnknownNotification = true)
        }
    }
}
