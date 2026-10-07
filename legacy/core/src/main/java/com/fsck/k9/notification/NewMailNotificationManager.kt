package com.fsck.k9.notification

import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.mailstore.LocalMessage
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

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
    private val accountManager: LegacyAccountManager,
    private val notificationIdRegistry: AccountNotificationIdRegistry,
    private val clock: Clock,
) {
    fun restoreNewMailNotifications(accountId: AccountId): NewMailNotificationData? {
        val notificationData = notificationRepository.restoreNotifications(accountId) ?: return null

        val addLockScreenNotification = notificationData.isSingleMessageNotification
        val singleNotificationDataList = notificationData.activeNotifications.map { notificationHolder ->
            createSingleNotificationData(
                accountId = accountId,
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
        accountId: AccountId,
        message: LocalMessage,
        silent: Boolean,
    ): NewMailNotificationData? {
        val account = accountManager.findById(accountId) ?: return null
        val isFromSelf = account.isAnIdentity(message.from)
        val content = contentCreator.createFromMessage(message, isFromSelf)

        return notificationRepository.addNotification(accountId, content, timestamp = now())?.let { result ->
            val singleNotificationData = createSingleNotificationData(
                accountId = accountId,
                notificationId = result.notificationHolder.notificationId,
                content = result.notificationHolder.content,
                timestamp = result.notificationHolder.timestamp,
                addLockScreenNotification = result.notificationData.isSingleMessageNotification,
            )

            NewMailNotificationData(
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
    }

    fun removeNewMailNotifications(
        accountId: AccountId,
        clearNewMessageState: Boolean,
        selector: (List<MessageReference>) -> List<MessageReference>,
    ): NewMailNotificationData? {
        val result = notificationRepository.removeNotifications(accountId, clearNewMessageState, selector)
            ?: return null

        val cancelNotificationIds = when {
            result.notificationData.isEmpty() -> {
                result.cancelNotificationIds + getNewMailSummaryNotificationId(accountId)
            }
            else -> {
                result.cancelNotificationIds
            }
        }

        val singleNotificationData = result.notificationHolders.map { notificationHolder ->
            createSingleNotificationData(
                accountId = accountId,
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

    fun clearNewMailNotifications(accountId: AccountId, clearNewMessageState: Boolean): List<Int> {
        notificationRepository.clearNotifications(accountId, clearNewMessageState)
        return notificationIdRegistry.getAllNewMailNotificationIds(accountId)
    }

    private fun getNewMailSummaryNotificationId(accountId: AccountId): Int {
        return notificationIdRegistry.getOrAllocate(accountId, AccountNotificationKind.NewMailSummary)
    }

    private fun createBaseNotificationData(notificationData: NotificationData): BaseNotificationData {
        return baseNotificationDataCreator.createBaseNotificationData(notificationData)
    }

    private fun createSingleNotificationData(
        accountId: AccountId,
        notificationId: Int,
        content: NotificationContent,
        timestamp: Long,
        addLockScreenNotification: Boolean,
    ): SingleNotificationData {
        return singleMessageNotificationDataCreator.createSingleNotificationData(
            accountId,
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
