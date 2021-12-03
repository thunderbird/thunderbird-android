package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.controller.MessageReference

internal class NotificationRepository(
    private val notificationStoreProvider: NotificationStoreProvider
) {
    private val notificationDataStore = NotificationDataStore()

    @Synchronized
    fun addNotification(account: Account, content: NotificationContent, timestamp: Long): AddNotificationResult {
        return notificationDataStore.addNotification(account, content, timestamp).also { result ->
            persistNotificationDataStoreChanges(account, result.notificationStoreOperations)
        }
    }

    @Synchronized
    fun removeNotification(account: Account, messageReference: MessageReference): RemoveNotificationResult? {
        return notificationDataStore.removeNotification(account, messageReference)?.also { result ->
            persistNotificationDataStoreChanges(account, result.notificationStoreOperations)
        }
    }

    @Synchronized
    fun clearNotifications(account: Account) {
        return notificationDataStore.clearNotifications(account).also {
            clearNotificationStore(account)
        }
    }

    private fun persistNotificationDataStoreChanges(account: Account, operations: List<NotificationStoreOperation>) {
        val notificationStore = notificationStoreProvider.getNotificationStore(account)
        notificationStore.persistNotificationChanges(operations)
    }

    private fun clearNotificationStore(account: Account) {
        val notificationStore = notificationStoreProvider.getNotificationStore(account)
        notificationStore.clearNotifications()
    }
}
