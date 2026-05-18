package com.fsck.k9.notification

import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.notification.NotificationActionTokens
import net.thunderbird.core.preference.NotificationQuickDelete
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager
import net.thunderbird.core.preference.notification.NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN
import net.thunderbird.core.preference.notification.NotificationPreferenceManager

internal class SingleMessageNotificationDataCreator(
    private val interactionPreferences: InteractionSettingsPreferenceManager,
    private val notificationPreference: NotificationPreferenceManager,
) {

    private val interactionSettings get() = interactionPreferences.getConfig()
    private val notificationSettings get() = notificationPreference.getConfig()

    fun createSingleNotificationData(
        account: LegacyAccountDto,
        notificationId: Int,
        content: NotificationContent,
        timestamp: Long,
        addLockScreenNotification: Boolean,
    ): SingleNotificationData {
        return SingleNotificationData(
            notificationId = notificationId,
            isSilent = true,
            timestamp = timestamp,
            content = content,
            actions = createSingleNotificationActions(account),
            wearActions = createSingleNotificationWearActions(account),
            addLockScreenNotification = addLockScreenNotification,
        )
    }

    fun createSummarySingleNotificationData(
        data: NotificationData,
        timestamp: Long,
        silent: Boolean,
    ): SummarySingleNotificationData {
        return SummarySingleNotificationData(
            SingleNotificationData(
                notificationId = NotificationIds.getNewMailSummaryNotificationId(data.account),
                isSilent = silent,
                timestamp = timestamp,
                content = data.activeNotifications.first().content,
                actions = createSingleNotificationActions(data.account),
                wearActions = createSingleNotificationWearActions(data.account),
                addLockScreenNotification = false,
            ),
        )
    }

    private fun createSingleNotificationActions(account: LegacyAccountDto): List<NotificationAction> {
        val order = parseActionsOrder(notificationSettings.messageActionsOrder)
        val cutoff = notificationSettings.messageActionsCutoff.coerceIn(
            0,
            NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN,
        )

        return resolveActions(
            order = order,
            cutoff = cutoff,
            hasArchiveFolder = account.hasArchiveFolder(),
            isDeleteEnabled = isDeleteActionEnabled(),
            hasSpamFolder = account.hasSpamFolder(),
            isSpamEnabled = !interactionSettings.isConfirmSpam,
        )
    }

    private fun createSingleNotificationWearActions(account: LegacyAccountDto): List<WearNotificationAction> {
        return buildList {
            add(WearNotificationAction.Reply)
            add(WearNotificationAction.MarkAsRead)

            if (isDeleteActionAvailableForWear()) {
                add(WearNotificationAction.Delete)
            }

            if (account.hasArchiveFolder()) {
                add(WearNotificationAction.Archive)
            }

            if (isSpamActionAvailableForWear(account)) {
                add(WearNotificationAction.Spam)
            }
        }
    }

    private fun resolveActions(
        order: List<NotificationAction>,
        cutoff: Int,
        hasArchiveFolder: Boolean,
        isDeleteEnabled: Boolean,
        hasSpamFolder: Boolean,
        isSpamEnabled: Boolean,
    ): List<NotificationAction> {
        val desired = order.take(cutoff).filter { action ->
            action.isAvailable(
                hasArchiveFolder = hasArchiveFolder,
                isDeleteEnabled = isDeleteEnabled,
                hasSpamFolder = hasSpamFolder,
                isSpamEnabled = isSpamEnabled,
            )
        }
        if (desired.size == NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN) return desired

        val filled = buildList {
            addAll(desired)
            for (action in order.drop(cutoff)) {
                if (size == NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN) break
                if (
                    action !in this &&
                    action.isAvailable(
                        hasArchiveFolder = hasArchiveFolder,
                        isDeleteEnabled = isDeleteEnabled,
                        hasSpamFolder = hasSpamFolder,
                        isSpamEnabled = isSpamEnabled,
                    )
                ) {
                    add(action)
                }
            }
        }

        return filled
    }

    private fun parseActionsOrder(tokens: List<String>): List<NotificationAction> {
        return NotificationActionTokens.normalizeOrder(
            persistedTokens = tokens,
            supportedActions = listOf(
                NotificationActionTokens.REPLY to NotificationAction.Reply,
                NotificationActionTokens.MARK_AS_READ to NotificationAction.MarkAsRead,
                NotificationActionTokens.DELETE to NotificationAction.Delete,
                NotificationActionTokens.STAR to NotificationAction.Star,
                NotificationActionTokens.ARCHIVE to NotificationAction.Archive,
                NotificationActionTokens.SPAM to NotificationAction.Spam,
            ),
        )
    }

    private fun isDeleteActionEnabled(): Boolean {
        return notificationSettings.notificationQuickDeleteBehaviour != NotificationQuickDelete.NEVER
    }

    // We don't support confirming actions on Wear devices. So don't show the action when confirmation is enabled.
    private fun isDeleteActionAvailableForWear(): Boolean {
        return !interactionSettings.isConfirmDeleteFromNotification
    }

    // We don't support confirming actions on Wear devices. So don't show the action when confirmation is enabled.
    private fun isSpamActionAvailableForWear(account: LegacyAccountDto): Boolean {
        return account.hasSpamFolder() && !interactionSettings.isConfirmSpam
    }
}

private fun NotificationAction.isAvailable(
    hasArchiveFolder: Boolean,
    isDeleteEnabled: Boolean,
    hasSpamFolder: Boolean,
    isSpamEnabled: Boolean,
): Boolean {
    return when (this) {
        NotificationAction.Reply -> true
        NotificationAction.MarkAsRead -> true
        NotificationAction.Delete -> isDeleteEnabled
        NotificationAction.Archive -> hasArchiveFolder
        NotificationAction.Spam -> hasSpamFolder && isSpamEnabled
        NotificationAction.Star -> true
    }
}
