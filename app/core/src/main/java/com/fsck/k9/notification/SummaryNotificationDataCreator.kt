package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.K9

internal class SummaryNotificationDataCreator(
    private val singleMessageNotificationDataCreator: SingleMessageNotificationDataCreator
) {
    fun createSummaryNotificationData(
        data: NotificationData,
        timestamp: Long,
        silent: Boolean
    ): SummaryNotificationData {
        val shouldBeSilent = silent || K9.isQuietTime
        return if (data.isSingleMessageNotification) {
            createSummarySingleNotificationData(data, timestamp, shouldBeSilent)
        } else {
            createSummaryInboxNotificationData(data, timestamp, shouldBeSilent)
        }
    }

    private fun createSummarySingleNotificationData(
        data: NotificationData,
        timestamp: Long,
        silent: Boolean
    ): SummaryNotificationData {
        return singleMessageNotificationDataCreator.createSummarySingleNotificationData(data, timestamp, silent)
    }

    private fun createSummaryInboxNotificationData(
        data: NotificationData,
        timestamp: Long,
        silent: Boolean
    ): SummaryNotificationData {
        return SummaryInboxNotificationData(
            notificationId = NotificationIds.getNewMailSummaryNotificationId(data.account),
            isSilent = silent,
            timestamp = timestamp,
            content = getSummaryContent(data),
            additionalMessagesCount = data.getSummaryOverflowMessagesCount(),
            messageReferences = data.getAllMessageReferences(),
            actions = createSummaryNotificationActions(),
            wearActions = createSummaryWearNotificationActions(data.account)
        )
    }

    private fun getSummaryContent(data: NotificationData): List<CharSequence> {
        return data.getContentForSummaryNotification().map { it.summary }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun createSummaryNotificationActions(): List<SummaryNotificationAction> {
        return buildList {
            add(SummaryNotificationAction.MarkAsRead)

            if (isDeleteActionEnabled()) {
                add(SummaryNotificationAction.Delete)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun createSummaryWearNotificationActions(account: Account): List<SummaryWearNotificationAction> {
        return buildList {
            add(SummaryWearNotificationAction.MarkAsRead)

            if (isDeleteActionAvailableForWear()) {
                add(SummaryWearNotificationAction.Delete)
            }

            if (account.hasArchiveFolder()) {
                add(SummaryWearNotificationAction.Archive)
            }
        }
    }

    private fun isDeleteActionEnabled(): Boolean {
        return K9.notificationQuickDeleteBehaviour == K9.NotificationQuickDelete.ALWAYS
    }

    // We don't support confirming actions on Wear devices. So don't show the action when confirmation is enabled.
    private fun isDeleteActionAvailableForWear(): Boolean {
        return isDeleteActionEnabled() && !K9.isConfirmDeleteFromNotification
    }
}
