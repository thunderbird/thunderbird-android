package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mailstore.LocalStoreProvider

internal class NotificationRepository(
    private val notificationStoreProvider: NotificationStoreProvider,
    private val localStoreProvider: LocalStoreProvider,
    private val notificationContentCreator: NotificationContentCreator
) {
    private val notificationDataStore = NotificationDataStore()

    @Synchronized
    fun restoreNotifications(account: Account): NotificationData? {
        val localStore = localStoreProvider.getInstance(account)

        val (activeNotificationMessages, inactiveNotificationMessages) = localStore.notificationMessages.partition {
            it.notificationId != null
        }

        if (activeNotificationMessages.isEmpty()) return null

        val activeNotifications = activeNotificationMessages.map { notificationMessage ->
            val content = notificationContentCreator.createFromMessage(account, notificationMessage.message)
            NotificationHolder(notificationMessage.notificationId!!, notificationMessage.timestamp, content)
        }

        val inactiveNotifications = inactiveNotificationMessages.map { notificationMessage ->
            val content = notificationContentCreator.createFromMessage(account, notificationMessage.message)
            InactiveNotificationHolder(notificationMessage.timestamp, content)
        }

        return notificationDataStore.initializeAccount(account, activeNotifications, inactiveNotifications)
    }

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
