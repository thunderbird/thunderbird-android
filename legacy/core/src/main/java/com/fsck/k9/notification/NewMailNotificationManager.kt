package com.fsck.k9.notification

import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.mailstore.LocalMessage
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import net.thunderbird.core.android.account.LegacyAccountDto

/**
 * Manages notifications for new messages
 */
internal class NewMailNotificationManager
@OptIn(ExperimentalTime::class)
constructor(
    private val contentCreator: NotificationContentCreator,
    private val notificationRepository: NotificationRepository,
    private val baseNotificationDataCreator: BaseNotificationDataCreator,
    private val singleMessageNotificationDataCreator: SingleMessageNotificationDataCreator,
    private val summaryNotificationDataCreator: SummaryNotificationDataCreator,
    private val clock: Clock,
) {
    fun restoreNewMailNotifications(account: LegacyAccountDto): NewMailNotificationData? {
        val notificationData = notificationRepository.restoreNotifications(account) ?: return null

        val addLockScreenNotification = notificationData.isSingleMessageNotification
        val singleNotificationDataList = notificationData.activeNotifications.map { notificationHolder ->
            createSingleNotificationData(
                account = account,
                notificationId = notificationHolder.notificationId,
                content = notificationHolder.content,
                timestamp = notificationHolder.timestamp,
                addLockScreenNotification = addLockScreenNotification,
            )
        }

        return NewMailNotificationData(
            cancelNotificationIds = emptyList(),
            baseNotificationData = createBaseNotificationData(notificationData),
            singleNotificationData = singleNotificationDataList,
            summaryNotificationData = createSummaryNotificationData(notificationData, silent = true),
        )
    }

    fun addNewMailNotification(
        account: LegacyAccountDto,
        message: LocalMessage,
        silent: Boolean,
    ): NewMailNotificationData? {
        val content = contentCreator.createFromMessage(account, message)

        val result = notificationRepository.addNotification(account, content, timestamp = now()) ?: return null

        val singleNotificationData = createSingleNotificationData(
            account = account,
            notificationId = result.notificationHolder.notificationId,
            content = result.notificationHolder.content,
            timestamp = result.notificationHolder.timestamp,
            addLockScreenNotification = result.notificationData.isSingleMessageNotification,
        )

        return NewMailNotificationData(
            cancelNotificationIds = if (result.shouldCancelNotification) {
                listOf(result.cancelNotificationId)
            } else {
                emptyList()
            },
            baseNotificationData = createBaseNotificationData(result.notificationData),
            singleNotificationData = listOf(singleNotificationData),
            summaryNotificationData = createSummaryNotificationData(result.notificationData, silent),
        )
    }

    fun removeNewMailNotifications(
        account: LegacyAccountDto,
        clearNewMessageState: Boolean,
        selector: (List<MessageReference>) -> List<MessageReference>,
    ): NewMailNotificationData? {
        val result = notificationRepository.removeNotifications(account, clearNewMessageState, selector) ?: return null

        val cancelNotificationIds = when {
            result.notificationData.isEmpty() -> {
                result.cancelNotificationIds + NotificationIds.getNewMailSummaryNotificationId(account)
            }
            else -> {
                result.cancelNotificationIds
            }
        }

        val singleNotificationData = result.notificationHolders.map { notificationHolder ->
            createSingleNotificationData(
                account = account,
                notificationId = notificationHolder.notificationId,
                content = notificationHolder.content,
                timestamp = notificationHolder.timestamp,
                addLockScreenNotification = result.notificationData.isSingleMessageNotification,
            )
        }

        return NewMailNotificationData(
            cancelNotificationIds = cancelNotificationIds,
            baseNotificationData = createBaseNotificationData(result.notificationData),
            singleNotificationData = singleNotificationData,
            summaryNotificationData = createSummaryNotificationData(result.notificationData, silent = true),
        )
    }

    fun clearNewMailNotifications(account: LegacyAccountDto, clearNewMessageState: Boolean): List<Int> {
        notificationRepository.clearNotifications(account, clearNewMessageState)
        return NotificationIds.getAllMessageNotificationIds(account)
    }

    private fun createBaseNotificationData(notificationData: NotificationData): BaseNotificationData {
        return baseNotificationDataCreator.createBaseNotificationData(notificationData)
    }

    private fun createSingleNotificationData(
        account: LegacyAccountDto,
        notificationId: Int,
        content: NotificationContent,
        timestamp: Long,
        addLockScreenNotification: Boolean,
    ): SingleNotificationData {
        return singleMessageNotificationDataCreator.createSingleNotificationData(
            account,
            notificationId,
            content,
            timestamp,
            addLockScreenNotification,
        )
    }

    private fun createSummaryNotificationData(data: NotificationData, silent: Boolean): SummaryNotificationData? {
        return if (data.isEmpty()) {
            null
        } else {
            summaryNotificationDataCreator.createSummaryNotificationData(data, silent)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun now(): Long = clock.now().toEpochMilliseconds()
}
