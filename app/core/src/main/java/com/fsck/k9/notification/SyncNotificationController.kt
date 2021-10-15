package com.fsck.k9.notification

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.Account
import com.fsck.k9.mailstore.LocalFolder

private const val NOTIFICATION_LED_WHILE_SYNCING = false

internal class SyncNotificationController(
    private val notificationHelper: NotificationHelper,
    private val actionBuilder: NotificationActionCreator,
    private val resourceProvider: NotificationResourceProvider
) {
    fun showSendingNotification(account: Account) {
        val accountName = notificationHelper.getAccountName(account)
        val title = resourceProvider.sendingMailTitle()
        val tickerText = resourceProvider.sendingMailBody(accountName)

        val notificationId = NotificationIds.getFetchingMailNotificationId(account)
        val outboxFolderId = account.outboxFolderId ?: error("Outbox folder not configured")
        val showMessageListPendingIntent = actionBuilder.createViewFolderPendingIntent(
            account, outboxFolderId, notificationId
        )

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconSendingMail)
            .setWhen(System.currentTimeMillis())
            .setOngoing(true)
            .setTicker(tickerText)
            .setContentTitle(title)
            .setContentText(accountName)
            .setContentIntent(showMessageListPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (NOTIFICATION_LED_WHILE_SYNCING) {
            notificationHelper.configureNotification(
                builder = notificationBuilder,
                ringtone = null,
                vibrationPattern = null,
                ledColor = account.notificationSetting.ledColor,
                ledSpeed = NotificationHelper.NOTIFICATION_LED_BLINK_FAST,
                ringAndVibrate = true
            )
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun clearSendingNotification(account: Account) {
        val notificationId = NotificationIds.getFetchingMailNotificationId(account)
        notificationManager.cancel(notificationId)
    }

    fun showFetchingMailNotification(account: Account, folder: LocalFolder) {
        val accountName = account.description
        val folderId = folder.databaseId
        val folderName = folder.name
        val tickerText = resourceProvider.checkingMailTicker(accountName, folderName)
        val title = resourceProvider.checkingMailTitle()

        // TODO: Use format string from resources
        val text = accountName + resourceProvider.checkingMailSeparator() + folderName

        val notificationId = NotificationIds.getFetchingMailNotificationId(account)
        val showMessageListPendingIntent = actionBuilder.createViewFolderPendingIntent(
            account, folderId, notificationId
        )

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconCheckingMail)
            .setWhen(System.currentTimeMillis())
            .setOngoing(true)
            .setTicker(tickerText)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(showMessageListPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (NOTIFICATION_LED_WHILE_SYNCING) {
            notificationHelper.configureNotification(
                builder = notificationBuilder,
                ringtone = null,
                vibrationPattern = null,
                ledColor = account.notificationSetting.ledColor,
                ledSpeed = NotificationHelper.NOTIFICATION_LED_BLINK_FAST,
                ringAndVibrate = true
            )
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun showEmptyFetchingMailNotification(account: Account) {
        val title = resourceProvider.checkingMailTitle()
        val text = account.description
        val notificationId = NotificationIds.getFetchingMailNotificationId(account)

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconCheckingMail)
            .setWhen(System.currentTimeMillis())
            .setOngoing(true)
            .setContentTitle(title)
            .setContentText(text)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (NOTIFICATION_LED_WHILE_SYNCING) {
            notificationHelper.configureNotification(
                builder = notificationBuilder,
                ringtone = null,
                vibrationPattern = null,
                ledColor = account.notificationSetting.ledColor,
                ledSpeed = NotificationHelper.NOTIFICATION_LED_BLINK_FAST,
                ringAndVibrate = true
            )
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun clearFetchingMailNotification(account: Account) {
        val notificationId = NotificationIds.getFetchingMailNotificationId(account)
        notificationManager.cancel(notificationId)
    }

    private val notificationManager: NotificationManagerCompat
        get() = notificationHelper.getNotificationManager()
}
