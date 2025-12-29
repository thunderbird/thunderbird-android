package com.fsck.k9.notification

import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager
import net.thunderbird.core.preference.notification.NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN

internal class SingleMessageNotificationDataCreator(
    private val interactionPreferences: InteractionSettingsPreferenceManager,
    private val generalSettingsManager: GeneralSettingsManager,
) {

    private val interactionSettings get() = interactionPreferences.getConfig()

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
        val notificationPrefs = generalSettingsManager.getConfig().notification
        val order = parseActionsOrder(notificationPrefs.messageActionsOrder)
        val cutoff = notificationPrefs.messageActionsCutoff.coerceIn(0, NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN)

        return resolveActions(
            order = order,
            cutoff = cutoff,
            hasArchiveFolder = account.hasArchiveFolder(),
            isDeleteEnabled = true,
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
        fun isAvailable(action: NotificationAction): Boolean {
            return when (action) {
                NotificationAction.Reply -> true
                NotificationAction.MarkAsRead -> true
                NotificationAction.Delete -> isDeleteEnabled
                NotificationAction.Archive -> hasArchiveFolder
                NotificationAction.Spam -> hasSpamFolder && isSpamEnabled
            }
        }

        val desired = order.take(cutoff).filter(::isAvailable)
        if (desired.size == NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN) return desired

        val filled = buildList {
            addAll(desired)
            for (action in order.drop(cutoff)) {
                if (size == NOTIFICATION_PREFERENCE_MAX_MESSAGE_ACTIONS_SHOWN) break
                if (action !in this && isAvailable(action)) {
                    add(action)
                }
            }
        }

        return filled
    }

    private fun parseActionsOrder(raw: String): List<NotificationAction> {
        val tokens = raw
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val seen = LinkedHashSet<NotificationAction>()
        for (token in tokens) {
            tokenToAction(token)?.let { seen.add(it) }
        }

        for (action in listOf(
            NotificationAction.Reply,
            NotificationAction.MarkAsRead,
            NotificationAction.Delete,
            NotificationAction.Archive,
            NotificationAction.Spam,
        )) {
            seen.add(action)
        }

        return seen.toList()
    }

    private fun tokenToAction(token: String): NotificationAction? {
        return when (token) {
            "reply" -> NotificationAction.Reply
            "mark_as_read" -> NotificationAction.MarkAsRead
            "delete" -> NotificationAction.Delete
            "archive" -> NotificationAction.Archive
            "spam" -> NotificationAction.Spam
            else -> null
        }
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
