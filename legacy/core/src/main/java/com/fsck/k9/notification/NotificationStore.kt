package com.fsck.k9.notification

interface NotificationStore {
    fun persistNotificationChanges(operations: List<NotificationStoreOperation>)
    fun clearNotifications()
}
