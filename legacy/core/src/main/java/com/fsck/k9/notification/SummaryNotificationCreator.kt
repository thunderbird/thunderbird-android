package com.fsck.k9.notification

import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.WearableExtender
import com.fsck.k9.notification.NotificationChannelManager.ChannelType
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.logging.legacy.Log
import androidx.core.app.NotificationCompat.Builder as NotificationBuilder

internal class SummaryNotificationCreator(
    private val notificationHelper: NotificationHelper,
    private val actionCreator: NotificationActionCreator,
    private val lockScreenNotificationCreator: LockScreenNotificationCreator,
    private val singleMessageNotificationCreator: SingleMessageNotificationCreator,
    private val resourceProvider: NotificationResourceProvider,
) {
    fun createSummaryNotification(
        baseNotificationData: BaseNotificationData,
        summaryNotificationData: SummaryNotificationData,
    ) {
        when (summaryNotificationData) {
            is SummarySingleNotificationData -> {
                createSingleMessageNotification(baseNotificationData, summaryNotificationData.singleNotificationData)
            }

            is SummaryInboxNotificationData -> {
                createInboxStyleSummaryNotification(baseNotificationData, summaryNotificationData)
            }
        }
    }

    private fun createSingleMessageNotification(
        baseNotificationData: BaseNotificationData,
        singleNotificationData: SingleNotificationData,
    ) {
        singleMessageNotificationCreator.createSingleNotification(
            baseNotificationData,
            singleNotificationData,
            isGroupSummary = true,
        )
    }

    private fun createInboxStyleSummaryNotification(
        baseNotificationData: BaseNotificationData,
        notificationData: SummaryInboxNotificationData,
    ) {
        val account = baseNotificationData.account
        val accountName = baseNotificationData.accountName
        val newMessagesCount = baseNotificationData.newMessagesCount
        val title = resourceProvider.newMessagesTitle(newMessagesCount)
        val summary = buildInboxSummaryText(accountName, notificationData)

        val notification = notificationHelper.createNotificationBuilder(account, ChannelType.MESSAGES)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
            .setGroup(baseNotificationData.groupKey)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setSmallIcon(resourceProvider.iconNewMail)
            .setColor(baseNotificationData.color)
            .setWhen(notificationData.timestamp)
            .setNumber(notificationData.additionalMessagesCount)
            .setTicker(notificationData.content.firstOrNull())
            .setContentTitle(title)
            .setSubText(accountName)
            .setInboxStyle(title, summary, notificationData.content)
            .setContentIntent(createViewIntent(account, notificationData))
            .setDeleteIntent(actionCreator.createDismissAllMessagesPendingIntent(account))
            .setDeviceActions(account, notificationData)
            .setWearActions(account, notificationData)
            .setAppearance(notificationData.isSilent, baseNotificationData.appearance)
            .setLockScreenNotification(baseNotificationData)
            .build()

        Log.v("Creating inbox-style summary notification (silent=%b): %s", notificationData.isSilent, notification)
        notificationHelper.notify(account, notificationData.notificationId, notification)
    }

    private fun buildInboxSummaryText(accountName: String, notificationData: SummaryInboxNotificationData): String {
        return if (notificationData.additionalMessagesCount > 0) {
            resourceProvider.additionalMessages(notificationData.additionalMessagesCount, accountName)
        } else {
            accountName
        }
    }

    private fun NotificationBuilder.setInboxStyle(
        title: String,
        summary: String,
        contentLines: List<CharSequence>,
    ) = apply {
        val style = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText(summary)

        for (line in contentLines) {
            style.addLine(line)
        }

        setStyle(style)
    }

    private fun createViewIntent(
        account: LegacyAccount,
        notificationData: SummaryInboxNotificationData,
    ): PendingIntent {
        return actionCreator.createViewMessagesPendingIntent(account, notificationData.messageReferences)
    }

    private fun NotificationBuilder.setDeviceActions(
        account: LegacyAccount,
        notificationData: SummaryInboxNotificationData,
    ) = apply {
        for (action in notificationData.actions) {
            when (action) {
                SummaryNotificationAction.MarkAsRead -> addMarkAllAsReadAction(account, notificationData)
                SummaryNotificationAction.Delete -> addDeleteAllAction(account, notificationData)
            }
        }
    }

    private fun NotificationBuilder.addMarkAllAsReadAction(
        account: LegacyAccount,
        notificationData: SummaryInboxNotificationData,
    ) {
        val icon = resourceProvider.iconMarkAsRead
        val title = resourceProvider.actionMarkAsRead()
        val messageReferences = notificationData.messageReferences
        val markAllAsReadPendingIntent = actionCreator.createMarkAllAsReadPendingIntent(account, messageReferences)

        addAction(icon, title, markAllAsReadPendingIntent)
    }

    private fun NotificationBuilder.addDeleteAllAction(
        account: LegacyAccount,
        notificationData: SummaryInboxNotificationData,
    ) {
        val icon = resourceProvider.iconDelete
        val title = resourceProvider.actionDelete()
        val messageReferences = notificationData.messageReferences
        val action = actionCreator.createDeleteAllPendingIntent(account, messageReferences)

        addAction(icon, title, action)
    }

    @Suppress("NestedBlockDepth")
    private fun NotificationBuilder.setWearActions(
        account: LegacyAccount,
        notificationData: SummaryInboxNotificationData,
    ) = apply {
        val wearableExtender = WearableExtender().apply {
            for (action in notificationData.wearActions) {
                when (action) {
                    SummaryWearNotificationAction.MarkAsRead -> addMarkAllAsReadAction(account, notificationData)
                    SummaryWearNotificationAction.Delete -> addDeleteAllAction(account, notificationData)
                    SummaryWearNotificationAction.Archive -> addArchiveAllAction(account, notificationData)
                }
            }
        }

        extend(wearableExtender)
    }

    private fun WearableExtender.addMarkAllAsReadAction(
        account: LegacyAccount,
        notificationData: SummaryInboxNotificationData,
    ) {
        val icon = resourceProvider.wearIconMarkAsRead
        val title = resourceProvider.actionMarkAllAsRead()
        val messageReferences = notificationData.messageReferences
        val action = actionCreator.createMarkAllAsReadPendingIntent(account, messageReferences)
        val markAsReadAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(markAsReadAction)
    }

    private fun WearableExtender.addDeleteAllAction(
        account: LegacyAccount,
        notificationData: SummaryInboxNotificationData,
    ) {
        val icon = resourceProvider.wearIconDelete
        val title = resourceProvider.actionDeleteAll()
        val messageReferences = notificationData.messageReferences
        val action = actionCreator.createDeleteAllPendingIntent(account, messageReferences)
        val deleteAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(deleteAction)
    }

    private fun WearableExtender.addArchiveAllAction(
        account: LegacyAccount,
        notificationData: SummaryInboxNotificationData,
    ) {
        val icon = resourceProvider.wearIconArchive
        val title = resourceProvider.actionArchiveAll()
        val messageReferences = notificationData.messageReferences
        val action = actionCreator.createArchiveAllPendingIntent(account, messageReferences)
        val archiveAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(archiveAction)
    }

    private fun NotificationBuilder.setLockScreenNotification(notificationData: BaseNotificationData) = apply {
        lockScreenNotificationCreator.configureLockScreenNotification(this, notificationData)
    }
}
