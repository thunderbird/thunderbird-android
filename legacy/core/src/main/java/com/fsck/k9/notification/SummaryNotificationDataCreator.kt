package com.fsck.k9.notification

import com.fsck.k9.K9
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.preference.GeneralSettingsManager

private const val MAX_NUMBER_OF_MESSAGES_FOR_SUMMARY_NOTIFICATION = 5

internal class SummaryNotificationDataCreator(
    private val singleMessageNotificationDataCreator: SingleMessageNotificationDataCreator,
    private val generalSettingsManager: GeneralSettingsManager,
) {
    private val interactionSettings get() = generalSettingsManager.getConfig().interaction

    fun createSummaryNotificationData(data: NotificationData, silent: Boolean): SummaryNotificationData {
        val timestamp = data.latestTimestamp
        val shouldBeSilent = silent || generalSettingsManager.getConfig().notification.isQuietTime
        return if (data.isSingleMessageNotification) {
            createSummarySingleNotificationData(data, timestamp, shouldBeSilent)
        } else {
            createSummaryInboxNotificationData(data, timestamp, shouldBeSilent)
        }
    }

    private fun createSummarySingleNotificationData(
        data: NotificationData,
        timestamp: Long,
        silent: Boolean,
    ): SummaryNotificationData {
        return singleMessageNotificationDataCreator.createSummarySingleNotificationData(data, timestamp, silent)
    }

    private fun createSummaryInboxNotificationData(
        data: NotificationData,
        timestamp: Long,
        silent: Boolean,
    ): SummaryNotificationData {
        return SummaryInboxNotificationData(
            notificationId = NotificationIds.getNewMailSummaryNotificationId(data.account),
            isSilent = silent,
            timestamp = timestamp,
            content = data.summaryContent,
            additionalMessagesCount = data.additionalMessagesCount,
            messageReferences = data.activeMessageReferences,
            actions = createSummaryNotificationActions(),
            wearActions = createSummaryWearNotificationActions(data.account),
        )
    }

    private fun createSummaryNotificationActions(): List<SummaryNotificationAction> {
        return buildList {
            add(SummaryNotificationAction.MarkAsRead)

            if (isDeleteActionEnabled()) {
                add(SummaryNotificationAction.Delete)
            }
        }
    }

    private fun createSummaryWearNotificationActions(account: LegacyAccountDto): List<SummaryWearNotificationAction> {
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
        return isDeleteActionEnabled() && !interactionSettings.isConfirmDeleteFromNotification
    }

    private val NotificationData.latestTimestamp: Long
        get() = activeNotifications.first().timestamp

    private val NotificationData.summaryContent: List<CharSequence>
        get() {
            return activeNotifications.asSequence()
                .map { it.content.summary }
                .take(MAX_NUMBER_OF_MESSAGES_FOR_SUMMARY_NOTIFICATION)
                .toList()
        }

    private val NotificationData.additionalMessagesCount: Int
        get() = (newMessagesCount - MAX_NUMBER_OF_MESSAGES_FOR_SUMMARY_NOTIFICATION).coerceAtLeast(0)
}
