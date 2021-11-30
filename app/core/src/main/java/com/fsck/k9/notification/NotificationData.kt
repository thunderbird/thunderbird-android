package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.controller.MessageReference
import java.util.LinkedList

/**
 * A holder class for pending new mail notifications.
 */
internal class NotificationData(val account: Account) {
    private val activeNotifications = LinkedList<NotificationHolder>()
    private val additionalNotifications = LinkedList<InactiveNotificationHolder>()
    private val notificationIdsInUse = mutableMapOf<Int, Boolean>()

    val newMessagesCount: Int
        get() = activeNotifications.size + additionalNotifications.size

    val isSingleMessageNotification: Boolean
        get() = activeNotifications.size == 1

    val holderForLatestNotification: NotificationHolder
        get() = activeNotifications.first

    private val isMaxNumberOfActiveNotificationsReached: Boolean
        get() = activeNotifications.size == MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS

    fun addNotificationContent(content: NotificationContent, timestamp: Long): AddNotificationResult {
        val notificationId: Int
        val cancelNotificationIdBeforeReuse: Boolean
        if (isMaxNumberOfActiveNotificationsReached) {
            val notificationHolder = activeNotifications.removeLast()
            addToAdditionalNotifications(notificationHolder)
            notificationId = notificationHolder.notificationId
            cancelNotificationIdBeforeReuse = true
        } else {
            notificationId = getNewNotificationId()
            cancelNotificationIdBeforeReuse = false
        }

        val notificationHolder = NotificationHolder(notificationId, timestamp, content)
        activeNotifications.addFirst(notificationHolder)

        return if (cancelNotificationIdBeforeReuse) {
            AddNotificationResult.replaceNotification(notificationHolder)
        } else {
            AddNotificationResult.newNotification(notificationHolder)
        }
    }

    private fun addToAdditionalNotifications(notificationHolder: NotificationHolder) {
        additionalNotifications.addFirst(
            InactiveNotificationHolder(notificationHolder.timestamp, notificationHolder.content)
        )
    }

    private fun getNewNotificationId(): Int {
        for (index in 0 until MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS) {
            val notificationId = NotificationIds.getSingleMessageNotificationId(account, index)
            if (!isNotificationInUse(notificationId)) {
                markNotificationIdAsInUse(notificationId)
                return notificationId
            }
        }

        throw AssertionError("getNewNotificationId() called with no free notification ID")
    }

    private fun isNotificationInUse(notificationId: Int): Boolean {
        return notificationIdsInUse[notificationId] ?: false
    }

    private fun markNotificationIdAsInUse(notificationId: Int) {
        notificationIdsInUse[notificationId] = true
    }

    private fun markNotificationIdAsFree(notificationId: Int) {
        notificationIdsInUse.remove(notificationId)
    }

    fun hasSummaryOverflowMessages(): Boolean {
        return activeNotifications.size > MAX_NUMBER_OF_MESSAGES_FOR_SUMMARY_NOTIFICATION
    }

    fun getSummaryOverflowMessagesCount(): Int {
        val activeOverflowCount = activeNotifications.size - MAX_NUMBER_OF_MESSAGES_FOR_SUMMARY_NOTIFICATION
        return if (activeOverflowCount > 0) {
            activeOverflowCount + additionalNotifications.size
        } else {
            additionalNotifications.size
        }
    }

    fun getContentForSummaryNotification(): List<NotificationContent> {
        return activeNotifications.asSequence()
            .map { it.content }
            .take(MAX_NUMBER_OF_MESSAGES_FOR_SUMMARY_NOTIFICATION)
            .toList()
    }

    fun getActiveNotificationIds(): List<Int> {
        return activeNotifications.map { it.notificationId }
    }

    fun removeNotificationForMessage(messageReference: MessageReference): RemoveNotificationResult {
        val holder = getNotificationHolderForMessage(messageReference)
            ?: return RemoveNotificationResult.unknownNotification()

        activeNotifications.remove(holder)

        val notificationId = holder.notificationId
        markNotificationIdAsFree(notificationId)

        return if (additionalNotifications.isEmpty()) {
            RemoveNotificationResult.cancelNotification(notificationId)
        } else {
            val replacementHolder = additionalNotifications.removeFirst()
            val replacement = NotificationHolder(notificationId, replacementHolder.timestamp, replacementHolder.content)
            activeNotifications.addLast(replacement)
            RemoveNotificationResult.createNotification(replacement)
        }
    }

    private fun getNotificationHolderForMessage(messageReference: MessageReference): NotificationHolder? {
        return activeNotifications.firstOrNull { it.content.messageReference == messageReference }
    }

    fun getAllMessageReferences(): ArrayList<MessageReference> {
        val newSize = activeNotifications.size + additionalNotifications.size
        val messageReferences = ArrayList<MessageReference>(newSize)

        for (holder in activeNotifications) {
            messageReferences.add(holder.content.messageReference)
        }

        for (holder in additionalNotifications) {
            messageReferences.add(holder.content.messageReference)
        }

        return messageReferences
    }

    companion object {
        // Note: As of Jellybean, phone notifications show a maximum of 5 lines, while tablet notifications show 7 lines.
        const val MAX_NUMBER_OF_MESSAGES_FOR_SUMMARY_NOTIFICATION = 5

        // Note: This class assumes that
        // MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS >= MAX_NUMBER_OF_MESSAGES_FOR_SUMMARY_NOTIFICATION
        const val MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS = 8
    }
}
