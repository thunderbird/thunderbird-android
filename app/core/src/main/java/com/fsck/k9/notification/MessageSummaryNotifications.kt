package com.fsck.k9.notification

import android.app.KeyguardManager
import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.K9.NotificationHideSubject
import com.fsck.k9.K9.NotificationQuickDelete
import com.fsck.k9.notification.NotificationGroupKeys.getGroupKey
import com.fsck.k9.notification.NotificationIds.getNewMailSummaryNotificationId

internal open class MessageSummaryNotifications(
    notificationHelper: NotificationHelper,
    actionCreator: NotificationActionCreator,
    private val lockScreenNotification: LockScreenNotification,
    private val singleMessageNotifications: SingleMessageNotifications,
    resourceProvider: NotificationResourceProvider
) : BaseNotifications(notificationHelper, actionCreator, resourceProvider) {

    fun buildSummaryNotification(account: Account, notificationData: NotificationData, silent: Boolean): Notification {
        val unreadMessageCount = notificationData.unreadMessageCount

        val builder = when {
            isPrivacyModeActive -> {
                createSimpleSummaryNotification(account, unreadMessageCount)
            }
            notificationData.isSingleMessageNotification -> {
                val holder = notificationData.holderForLatestNotification
                createSingleMessageNotification(account, holder)
            }
            else -> {
                createInboxStyleSummaryNotification(account, notificationData, unreadMessageCount)
            }
        }

        if (notificationData.containsStarredMessages()) {
            builder.priority = NotificationCompat.PRIORITY_HIGH
        }

        val notificationId = getNewMailSummaryNotificationId(account)
        val deletePendingIntent = actionCreator.createDismissAllMessagesPendingIntent(account, notificationId)
        builder.setDeleteIntent(deletePendingIntent)

        lockScreenNotification.configureLockScreenNotification(builder, notificationData)

        val notificationSetting = account.notificationSetting
        notificationHelper.configureNotification(
            builder = builder,
            ringtone = if (notificationSetting.isRingEnabled) notificationSetting.ringtone else null,
            vibrationPattern = if (notificationSetting.isVibrateEnabled) notificationSetting.vibration else null,
            ledColor = if (notificationSetting.isLedEnabled) notificationSetting.ledColor else null,
            ledSpeed = NotificationHelper.NOTIFICATION_LED_BLINK_SLOW,
            ringAndVibrate = !silent
        )

        return builder.build()
    }

    private fun createSimpleSummaryNotification(account: Account, unreadMessageCount: Int): NotificationCompat.Builder {
        val accountName = notificationHelper.getAccountName(account)
        val newMailText = resourceProvider.newMailTitle()
        val unreadMessageCountText = resourceProvider.newMailUnreadMessageCount(unreadMessageCount, accountName)
        val notificationId = getNewMailSummaryNotificationId(account)
        val contentIntent = actionCreator.createViewFolderListPendingIntent(account, notificationId)

        return createAndInitializeNotificationBuilder(account)
            .setNumber(unreadMessageCount)
            .setTicker(newMailText)
            .setContentTitle(unreadMessageCountText)
            .setContentText(newMailText)
            .setContentIntent(contentIntent)
    }

    private fun createSingleMessageNotification(
        account: Account,
        holder: NotificationHolder
    ): NotificationCompat.Builder {
        val notificationId = getNewMailSummaryNotificationId(account)
        val builder = singleMessageNotifications.createSingleMessageNotificationBuilder(account, holder, notificationId)
        builder.setGroupSummary(true)

        return builder
    }

    private fun createInboxStyleSummaryNotification(
        account: Account,
        notificationData: NotificationData,
        unreadMessageCount: Int
    ): NotificationCompat.Builder {
        val latestNotification = notificationData.holderForLatestNotification
        val newMessagesCount = notificationData.newMessagesCount
        val accountName = notificationHelper.getAccountName(account)
        val title = resourceProvider.newMessagesTitle(newMessagesCount)
        val summary = if (notificationData.hasSummaryOverflowMessages()) {
            resourceProvider.additionalMessages(notificationData.getSummaryOverflowMessagesCount(), accountName)
        } else {
            accountName
        }
        val groupKey = getGroupKey(account)

        val builder = createAndInitializeNotificationBuilder(account)
            .setNumber(unreadMessageCount)
            .setTicker(latestNotification.content.summary)
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setContentTitle(title)
            .setSubText(accountName)

        val style = createInboxStyle(builder)
            .setBigContentTitle(title)
            .setSummaryText(summary)

        for (content in notificationData.getContentForSummaryNotification()) {
            style.addLine(content.summary)
        }
        builder.setStyle(style)

        addMarkAllAsReadAction(builder, notificationData)
        addDeleteAllAction(builder, notificationData)
        addWearActions(builder, notificationData)

        val notificationId = getNewMailSummaryNotificationId(account)
        val messageReferences = notificationData.getAllMessageReferences()
        val contentIntent = actionCreator.createViewMessagesPendingIntent(account, messageReferences, notificationId)
        builder.setContentIntent(contentIntent)

        return builder
    }

    private fun addMarkAllAsReadAction(builder: NotificationCompat.Builder, notificationData: NotificationData) {
        val icon = resourceProvider.iconMarkAsRead
        val title = resourceProvider.actionMarkAsRead()
        val account = notificationData.account
        val messageReferences = notificationData.getAllMessageReferences()
        val notificationId = getNewMailSummaryNotificationId(account)
        val markAllAsReadPendingIntent =
            actionCreator.createMarkAllAsReadPendingIntent(account, messageReferences, notificationId)

        builder.addAction(icon, title, markAllAsReadPendingIntent)
    }

    private fun addDeleteAllAction(builder: NotificationCompat.Builder, notificationData: NotificationData) {
        if (K9.notificationQuickDeleteBehaviour !== NotificationQuickDelete.ALWAYS) {
            return
        }

        val icon = resourceProvider.iconDelete
        val title = resourceProvider.actionDelete()
        val account = notificationData.account
        val notificationId = getNewMailSummaryNotificationId(account)
        val messageReferences = notificationData.getAllMessageReferences()
        val action = actionCreator.createDeleteAllPendingIntent(account, messageReferences, notificationId)

        builder.addAction(icon, title, action)
    }

    private fun addWearActions(builder: NotificationCompat.Builder, notificationData: NotificationData) {
        val wearableExtender = NotificationCompat.WearableExtender()

        addMarkAllAsReadWearAction(wearableExtender, notificationData)

        if (isDeleteActionAvailableForWear()) {
            addDeleteAllWearAction(wearableExtender, notificationData)
        }

        if (isArchiveActionAvailableForWear(notificationData.account)) {
            addArchiveAllWearAction(wearableExtender, notificationData)
        }

        builder.extend(wearableExtender)
    }

    private fun addMarkAllAsReadWearAction(
        wearableExtender: NotificationCompat.WearableExtender,
        notificationData: NotificationData
    ) {
        val icon = resourceProvider.wearIconMarkAsRead
        val title = resourceProvider.actionMarkAllAsRead()
        val account = notificationData.account
        val messageReferences = notificationData.getAllMessageReferences()
        val notificationId = getNewMailSummaryNotificationId(account)
        val action = actionCreator.createMarkAllAsReadPendingIntent(account, messageReferences, notificationId)
        val markAsReadAction = NotificationCompat.Action.Builder(icon, title, action).build()

        wearableExtender.addAction(markAsReadAction)
    }

    private fun addDeleteAllWearAction(
        wearableExtender: NotificationCompat.WearableExtender,
        notificationData: NotificationData
    ) {
        val icon = resourceProvider.wearIconDelete
        val title = resourceProvider.actionDeleteAll()
        val account = notificationData.account
        val messageReferences = notificationData.getAllMessageReferences()
        val notificationId = getNewMailSummaryNotificationId(account)
        val action = actionCreator.createDeleteAllPendingIntent(account, messageReferences, notificationId)
        val deleteAction = NotificationCompat.Action.Builder(icon, title, action).build()

        wearableExtender.addAction(deleteAction)
    }

    private fun addArchiveAllWearAction(
        wearableExtender: NotificationCompat.WearableExtender,
        notificationData: NotificationData
    ) {
        val icon = resourceProvider.wearIconArchive
        val title = resourceProvider.actionArchiveAll()
        val account = notificationData.account
        val messageReferences = notificationData.getAllMessageReferences()
        val notificationId = getNewMailSummaryNotificationId(account)
        val action = actionCreator.createArchiveAllPendingIntent(account, messageReferences, notificationId)
        val archiveAction = NotificationCompat.Action.Builder(icon, title, action).build()

        wearableExtender.addAction(archiveAction)
    }

    private fun isDeleteActionAvailableForWear(): Boolean {
        return isDeleteActionEnabled() && !K9.isConfirmDeleteFromNotification
    }

    private fun isArchiveActionAvailableForWear(account: Account): Boolean {
        return account.archiveFolderId != null
    }

    private val isPrivacyModeActive: Boolean
        get() {
            val keyguardService = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val privacyModeAlwaysEnabled = K9.notificationHideSubject === NotificationHideSubject.ALWAYS
            val privacyModeEnabledWhenLocked = K9.notificationHideSubject === NotificationHideSubject.WHEN_LOCKED
            val screenLocked = keyguardService.inKeyguardRestrictedInputMode()
            return privacyModeAlwaysEnabled || privacyModeEnabledWhenLocked && screenLocked
        }

    protected open fun createInboxStyle(builder: NotificationCompat.Builder?): NotificationCompat.InboxStyle {
        return NotificationCompat.InboxStyle(builder)
    }
}
