package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.controller.MessageReference

internal const val MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS = 8

/**
 * Stores information about new message notifications for all accounts.
 *
 * We only use a limited number of system notifications per account (see [MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS]);
 * those are called active notifications. The rest are called inactive notifications. When an active notification is
 * removed, the latest inactive notification is promoted to an active notification.
 */
internal class NotificationDataStore {
    private val notificationDataMap = mutableMapOf<String, NotificationData>()

    @Synchronized
    fun initializeAccount(
        account: Account,
        activeNotifications: List<NotificationHolder>,
        inactiveNotifications: List<InactiveNotificationHolder>
    ): NotificationData {
        require(activeNotifications.size <= MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS)

        return NotificationData(account, activeNotifications, inactiveNotifications).also { notificationData ->
            notificationDataMap[account.uuid] = notificationData
        }
    }

    @Synchronized
    fun addNotification(account: Account, content: NotificationContent, timestamp: Long): AddNotificationResult {
        val notificationData = getNotificationData(account)

        return if (notificationData.isMaxNumberOfActiveNotificationsReached) {
            val lastNotificationHolder = notificationData.activeNotifications.last()
            val inactiveNotificationHolder = lastNotificationHolder.toInactiveNotificationHolder()

            val notificationId = lastNotificationHolder.notificationId
            val notificationHolder = NotificationHolder(notificationId, timestamp, content)

            val operations = listOf(
                NotificationStoreOperation.ChangeToInactive(lastNotificationHolder.content.messageReference),
                NotificationStoreOperation.Add(content.messageReference, notificationId, timestamp)
            )

            val newNotificationData = notificationData.copy(
                activeNotifications = listOf(notificationHolder) + notificationData.activeNotifications.dropLast(1),
                inactiveNotifications = listOf(inactiveNotificationHolder) + notificationData.inactiveNotifications
            )
            notificationDataMap[account.uuid] = newNotificationData

            AddNotificationResult.replaceNotification(newNotificationData, operations, notificationHolder)
        } else {
            val notificationId = notificationData.getNewNotificationId()
            val notificationHolder = NotificationHolder(notificationId, timestamp, content)

            val operations = listOf(
                NotificationStoreOperation.Add(content.messageReference, notificationId, timestamp)
            )

            val newNotificationData = notificationData.copy(
                activeNotifications = listOf(notificationHolder) + notificationData.activeNotifications
            )
            notificationDataMap[account.uuid] = newNotificationData

            AddNotificationResult.newNotification(newNotificationData, operations, notificationHolder)
        }
    }

    @Synchronized
    fun removeNotification(account: Account, messageReference: MessageReference): RemoveNotificationResult? {
        val notificationData = getNotificationData(account)
        if (notificationData.isEmpty()) return null

        val notificationHolder = notificationData.activeNotifications.firstOrNull {
            it.content.messageReference == messageReference
        }

        return if (notificationHolder == null) {
            val inactiveNotificationHolder = notificationData.inactiveNotifications.firstOrNull {
                it.content.messageReference == messageReference
            } ?: return null

            val operations = listOf(
                NotificationStoreOperation.Remove(messageReference)
            )

            val newNotificationData = notificationData.copy(
                inactiveNotifications = notificationData.inactiveNotifications - inactiveNotificationHolder
            )
            notificationDataMap[account.uuid] = newNotificationData

            RemoveNotificationResult.recreateSummaryNotification(newNotificationData, operations)
        } else if (notificationData.inactiveNotifications.isNotEmpty()) {
            val newNotificationHolder = notificationData.inactiveNotifications.first()
                .toNotificationHolder(notificationHolder.notificationId)

            val operations = listOf(
                NotificationStoreOperation.Remove(messageReference),
                NotificationStoreOperation.ChangeToActive(
                    newNotificationHolder.content.messageReference,
                    newNotificationHolder.notificationId
                )
            )

            val newNotificationData = notificationData.copy(
                activeNotifications = notificationData.activeNotifications - notificationHolder + newNotificationHolder,
                inactiveNotifications = notificationData.inactiveNotifications.drop(1)
            )
            notificationDataMap[account.uuid] = newNotificationData

            RemoveNotificationResult.replaceNotification(newNotificationData, operations, newNotificationHolder)
        } else {
            val operations = listOf(
                NotificationStoreOperation.Remove(messageReference),
            )

            val newNotificationData = notificationData.copy(
                activeNotifications = notificationData.activeNotifications - notificationHolder
            )
            notificationDataMap[account.uuid] = newNotificationData

            RemoveNotificationResult.cancelNotification(
                newNotificationData,
                operations,
                notificationHolder.notificationId
            )
        }
    }

    @Synchronized
    fun clearNotifications(account: Account) {
        notificationDataMap.remove(account.uuid)
    }

    private fun getNotificationData(account: Account): NotificationData {
        return notificationDataMap[account.uuid] ?: NotificationData.create(account).also { notificationData ->
            notificationDataMap[account.uuid] = notificationData
        }
    }

    private val NotificationData.isMaxNumberOfActiveNotificationsReached: Boolean
        get() = activeNotifications.size == MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS

    private fun NotificationData.getNewNotificationId(): Int {
        val notificationIdsInUse = activeNotifications.map { it.notificationId }.toSet()
        for (index in 0 until MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS) {
            val notificationId = NotificationIds.getSingleMessageNotificationId(account, index)
            if (notificationId !in notificationIdsInUse) {
                return notificationId
            }
        }

        throw AssertionError("getNewNotificationId() called with no free notification ID")
    }

    private fun NotificationHolder.toInactiveNotificationHolder() = InactiveNotificationHolder(timestamp, content)

    private fun InactiveNotificationHolder.toNotificationHolder(notificationId: Int): NotificationHolder {
        return NotificationHolder(notificationId, timestamp, content)
    }
}
