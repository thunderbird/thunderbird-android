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
    fun addNotification(account: Account, content: NotificationContent, timestamp: Long): AddNotificationResult? {
        val notificationData = getNotificationData(account)
        val messageReference = content.messageReference

        val activeNotification = notificationData.activeNotifications.firstOrNull { notificationHolder ->
            notificationHolder.content.messageReference == messageReference
        }
        val inactiveNotification = notificationData.inactiveNotifications.firstOrNull { inactiveNotificationHolder ->
            inactiveNotificationHolder.content.messageReference == messageReference
        }

        return if (activeNotification != null) {
            val newActiveNotification = activeNotification.copy(content = content)
            val notificationHolder = activeNotification.copy(
                content = content
            )

            val operations = emptyList<NotificationStoreOperation>()

            val newActiveNotifications = notificationData.activeNotifications
                .replace(activeNotification, newActiveNotification)
            val newNotificationData = notificationData.copy(
                activeNotifications = newActiveNotifications
            )
            notificationDataMap[account.uuid] = newNotificationData

            AddNotificationResult.newNotification(newNotificationData, operations, notificationHolder)
        } else if (inactiveNotification != null) {
            val newInactiveNotification = inactiveNotification.copy(content = content)
            val newInactiveNotifications = notificationData.inactiveNotifications
                .replace(inactiveNotification, newInactiveNotification)

            val newNotificationData = notificationData.copy(
                inactiveNotifications = newInactiveNotifications
            )
            notificationDataMap[account.uuid] = newNotificationData

            null
        } else if (notificationData.isMaxNumberOfActiveNotificationsReached) {
            val lastNotificationHolder = notificationData.activeNotifications.last()
            val inactiveNotificationHolder = lastNotificationHolder.toInactiveNotificationHolder()

            val notificationId = lastNotificationHolder.notificationId
            val notificationHolder = NotificationHolder(notificationId, timestamp, content)

            val operations = listOf(
                NotificationStoreOperation.ChangeToInactive(lastNotificationHolder.content.messageReference),
                NotificationStoreOperation.Add(messageReference, notificationId, timestamp)
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
                NotificationStoreOperation.Add(messageReference, notificationId, timestamp)
            )

            val newNotificationData = notificationData.copy(
                activeNotifications = listOf(notificationHolder) + notificationData.activeNotifications
            )
            notificationDataMap[account.uuid] = newNotificationData

            AddNotificationResult.newNotification(newNotificationData, operations, notificationHolder)
        }
    }

    @Synchronized
    fun removeNotifications(
        account: Account,
        selector: (List<MessageReference>) -> List<MessageReference>
    ): RemoveNotificationsResult? {
        val notificationData = getNotificationData(account)
        if (notificationData.isEmpty()) return null

        val removeMessageReferences = selector.invoke(notificationData.messageReferences)

        val operations = mutableListOf<NotificationStoreOperation>()
        val newNotificationHolders = mutableListOf<NotificationHolder>()
        val cancelNotificationIds = mutableListOf<Int>()

        for (messageReference in removeMessageReferences) {
            val notificationHolder = notificationData.activeNotifications.firstOrNull {
                it.content.messageReference == messageReference
            }

            if (notificationHolder == null) {
                val inactiveNotificationHolder = notificationData.inactiveNotifications.firstOrNull {
                    it.content.messageReference == messageReference
                } ?: continue

                operations.add(NotificationStoreOperation.Remove(messageReference))

                val newNotificationData = notificationData.copy(
                    inactiveNotifications = notificationData.inactiveNotifications - inactiveNotificationHolder
                )
                notificationDataMap[account.uuid] = newNotificationData
            } else if (notificationData.inactiveNotifications.isNotEmpty()) {
                val newNotificationHolder = notificationData.inactiveNotifications.first()
                    .toNotificationHolder(notificationHolder.notificationId)

                newNotificationHolders.add(newNotificationHolder)
                cancelNotificationIds.add(notificationHolder.notificationId)

                operations.add(NotificationStoreOperation.Remove(messageReference))
                operations.add(
                    NotificationStoreOperation.ChangeToActive(
                        newNotificationHolder.content.messageReference,
                        newNotificationHolder.notificationId
                    )
                )

                val newNotificationData = notificationData.copy(
                    activeNotifications = notificationData.activeNotifications - notificationHolder +
                        newNotificationHolder,
                    inactiveNotifications = notificationData.inactiveNotifications.drop(1)
                )
                notificationDataMap[account.uuid] = newNotificationData
            } else {
                cancelNotificationIds.add(notificationHolder.notificationId)

                operations.add(NotificationStoreOperation.Remove(messageReference))

                val newNotificationData = notificationData.copy(
                    activeNotifications = notificationData.activeNotifications - notificationHolder
                )
                notificationDataMap[account.uuid] = newNotificationData
            }
        }

        return if (operations.isEmpty()) {
            null
        } else {
            RemoveNotificationsResult(
                notificationData = getNotificationData(account),
                notificationStoreOperations = operations,
                notificationHolders = newNotificationHolders,
                cancelNotificationIds = cancelNotificationIds
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

    private fun <T> List<T>.replace(old: T, new: T): List<T> {
        return map { element ->
            if (element === old) new else element
        }
    }
}
