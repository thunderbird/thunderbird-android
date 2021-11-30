package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.Clock
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mailstore.LocalMessage

/**
 * Manages notifications for new messages
 */
internal class NewMailNotificationManager(
    private val contentCreator: NotificationContentCreator,
    private val baseNotificationDataCreator: BaseNotificationDataCreator,
    private val singleMessageNotificationDataCreator: SingleMessageNotificationDataCreator,
    private val summaryNotificationDataCreator: SummaryNotificationDataCreator,
    private val clock: Clock
) {
    private val notifications = mutableMapOf<Int, NotificationData>()
    private val lock = Any()

    fun addNewMailNotification(account: Account, message: LocalMessage, silent: Boolean): NewMailNotificationData {
        val content = contentCreator.createFromMessage(account, message)

        synchronized(lock) {
            val notificationData = getOrCreateNotificationData(account)
            val result = notificationData.addNotificationContent(content, timestamp = now())

            val singleNotificationData = createSingleNotificationData(
                account = account,
                notificationId = result.notificationHolder.notificationId,
                content = result.notificationHolder.content,
                timestamp = result.notificationHolder.timestamp,
                addLockScreenNotification = notificationData.isSingleMessageNotification
            )

            return NewMailNotificationData(
                cancelNotificationIds = if (result.shouldCancelNotification) {
                    listOf(result.notificationId)
                } else {
                    emptyList()
                },
                baseNotificationData = createBaseNotificationData(notificationData),
                singleNotificationData = listOf(singleNotificationData),
                summaryNotificationData = createSummaryNotificationData(notificationData, silent)
            )
        }
    }

    fun removeNewMailNotification(account: Account, messageReference: MessageReference): NewMailNotificationData? {
        synchronized(lock) {
            val notificationData = getNotificationData(account) ?: return null

            val result = notificationData.removeNotificationForMessage(messageReference)
            if (result.isUnknownNotification) return null

            if (notificationData.newMessagesCount == 0) {
                return NewMailNotificationData(
                    cancelNotificationIds = listOf(
                        NotificationIds.getNewMailSummaryNotificationId(account),
                        result.notificationId
                    ),
                    baseNotificationData = createBaseNotificationData(notificationData),
                    singleNotificationData = emptyList(),
                    summaryNotificationData = null
                )
            }

            val singleNotificationData = if (result.shouldCreateNotification) {
                val singleNotificationData = createSingleNotificationData(
                    account = account,
                    notificationId = result.notificationHolder.notificationId,
                    content = result.notificationHolder.content,
                    timestamp = result.notificationHolder.timestamp,
                    addLockScreenNotification = notificationData.isSingleMessageNotification
                )
                listOf(singleNotificationData)
            } else {
                emptyList()
            }

            return NewMailNotificationData(
                cancelNotificationIds = listOf(result.notificationId),
                baseNotificationData = createBaseNotificationData(notificationData),
                singleNotificationData = singleNotificationData,
                summaryNotificationData = createSummaryNotificationData(notificationData, silent = true)
            )
        }
    }

    fun clearNewMailNotifications(account: Account): List<Int> {
        synchronized(lock) {
            val notificationData = removeNotificationData(account) ?: return emptyList()
            return notificationData.getActiveNotificationIds() +
                NotificationIds.getNewMailSummaryNotificationId(account)
        }
    }

    private fun createBaseNotificationData(notificationData: NotificationData): BaseNotificationData {
        return baseNotificationDataCreator.createBaseNotificationData(notificationData)
    }

    private fun createSingleNotificationData(
        account: Account,
        notificationId: Int,
        content: NotificationContent,
        timestamp: Long,
        addLockScreenNotification: Boolean
    ): SingleNotificationData {
        return singleMessageNotificationDataCreator.createSingleNotificationData(
            account,
            notificationId,
            content,
            timestamp,
            addLockScreenNotification
        )
    }

    private fun createSummaryNotificationData(data: NotificationData, silent: Boolean): SummaryNotificationData {
        return summaryNotificationDataCreator.createSummaryNotificationData(data, silent)
    }

    private fun getOrCreateNotificationData(account: Account): NotificationData {
        val notificationData = getNotificationData(account)
        if (notificationData != null) return notificationData

        val accountNumber = account.accountNumber
        val newNotificationHolder = NotificationData(account)
        notifications[accountNumber] = newNotificationHolder

        return newNotificationHolder
    }

    private fun getNotificationData(account: Account): NotificationData? {
        val accountNumber = account.accountNumber
        return notifications[accountNumber]
    }

    private fun removeNotificationData(account: Account): NotificationData? {
        val accountNumber = account.accountNumber
        val notificationData = notifications[accountNumber]
        notifications.remove(accountNumber)
        return notificationData
    }

    private fun now(): Long = clock.time
}
