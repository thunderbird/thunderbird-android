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
    private val notificationRepository: NotificationRepository,
    private val baseNotificationDataCreator: BaseNotificationDataCreator,
    private val singleMessageNotificationDataCreator: SingleMessageNotificationDataCreator,
    private val summaryNotificationDataCreator: SummaryNotificationDataCreator,
    private val clock: Clock
) {
    fun addNewMailNotification(account: Account, message: LocalMessage, silent: Boolean): NewMailNotificationData {
        val content = contentCreator.createFromMessage(account, message)

        val result = notificationRepository.addNotification(account, content, timestamp = now())

        val singleNotificationData = createSingleNotificationData(
            account = account,
            notificationId = result.notificationHolder.notificationId,
            content = result.notificationHolder.content,
            timestamp = result.notificationHolder.timestamp,
            addLockScreenNotification = result.notificationData.isSingleMessageNotification
        )

        return NewMailNotificationData(
            cancelNotificationIds = if (result.shouldCancelNotification) {
                listOf(result.cancelNotificationId)
            } else {
                emptyList()
            },
            baseNotificationData = createBaseNotificationData(result.notificationData),
            singleNotificationData = listOf(singleNotificationData),
            summaryNotificationData = createSummaryNotificationData(result.notificationData, silent)
        )
    }

    fun removeNewMailNotification(account: Account, messageReference: MessageReference): NewMailNotificationData? {
        val result = notificationRepository.removeNotification(account, messageReference) ?: return null

        val cancelNotificationIds = when {
            result.shouldCancelNotification && result.notificationData.isEmpty() -> {
                listOf(NotificationIds.getNewMailSummaryNotificationId(account), result.cancelNotificationId)
            }
            result.shouldCancelNotification -> {
                listOf(result.cancelNotificationId)
            }
            else -> {
                emptyList()
            }
        }

        val singleNotificationDataList = if (result.shouldCreateNotification) {
            listOf(
                createSingleNotificationData(
                    account = account,
                    notificationId = result.notificationHolder.notificationId,
                    content = result.notificationHolder.content,
                    timestamp = result.notificationHolder.timestamp,
                    addLockScreenNotification = result.notificationData.isSingleMessageNotification
                )
            )
        } else {
            emptyList()
        }

        return NewMailNotificationData(
            cancelNotificationIds = cancelNotificationIds,
            baseNotificationData = createBaseNotificationData(result.notificationData),
            singleNotificationData = singleNotificationDataList,
            summaryNotificationData = createSummaryNotificationData(result.notificationData, silent = true)
        )
    }

    fun clearNewMailNotifications(account: Account): List<Int> {
        notificationRepository.clearNotifications(account)
        return NotificationIds.getAllMessageNotificationIds(account)
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

    private fun createSummaryNotificationData(data: NotificationData, silent: Boolean): SummaryNotificationData? {
        return if (data.isEmpty()) {
            null
        } else {
            summaryNotificationDataCreator.createSummaryNotificationData(data, silent)
        }
    }

    private fun now(): Long = clock.time
}
