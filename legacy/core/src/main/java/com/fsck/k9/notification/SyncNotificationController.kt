package com.fsck.k9.notification

import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.mailstore.LocalFolder
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager

internal class SyncNotificationController(
    private val notificationHelper: NotificationHelper,
    private val actionBuilder: NotificationActionCreator,
    private val resourceProvider: NotificationResourceProvider,
    private val outboxFolderManager: OutboxFolderManager,
) {
    fun showSendingNotification(account: LegacyAccountDto) {
        val accountName = account.displayName
        val title = resourceProvider.sendingMailTitle()
        val tickerText = resourceProvider.sendingMailBody(accountName)

        val notificationId = NotificationIds.getFetchingMailNotificationId(account)
        val outboxFolderId = outboxFolderManager
            .getOutboxFolderIdSync(account.id)
            .takeIf { it != -1L }
            ?: error("Outbox folder not configured")
        val showMessageListPendingIntent = actionBuilder.createViewFolderPendingIntent(account, outboxFolderId)

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconSendingMail)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setOngoing(true)
            .setTicker(tickerText)
            .setContentTitle(title)
            .setContentText(accountName)
            .setContentIntent(showMessageListPendingIntent)
            .setPublicVersion(createSendingLockScreenNotification(account))

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun clearSendingNotification(account: LegacyAccountDto) {
        val notificationId = NotificationIds.getFetchingMailNotificationId(account)
        notificationManager.cancel(notificationId)
    }

    fun showFetchingMailNotification(account: LegacyAccountDto, folder: LocalFolder) {
        val accountName = account.displayName
        val folderId = folder.databaseId
        val folderName = folder.name
        val tickerText = resourceProvider.checkingMailTicker(accountName, folderName)
        val title = resourceProvider.checkingMailTitle()

        // TODO: Use format string from resources
        val text = accountName + resourceProvider.checkingMailSeparator() + folderName

        val notificationId = NotificationIds.getFetchingMailNotificationId(account)
        val showMessageListPendingIntent = actionBuilder.createViewFolderPendingIntent(account, folderId)

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconCheckingMail)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setOngoing(true)
            .setTicker(tickerText)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(showMessageListPendingIntent)
            .setPublicVersion(createFetchingMailLockScreenNotification(account))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun showEmptyFetchingMailNotification(account: LegacyAccountDto) {
        val title = resourceProvider.checkingMailTitle()
        val text = account.displayName
        val notificationId = NotificationIds.getFetchingMailNotificationId(account)

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconCheckingMail)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setOngoing(true)
            .setContentTitle(title)
            .setContentText(text)
            .setPublicVersion(createFetchingMailLockScreenNotification(account))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun clearFetchingMailNotification(account: LegacyAccountDto) {
        val notificationId = NotificationIds.getFetchingMailNotificationId(account)
        notificationManager.cancel(notificationId)
    }

    private fun createSendingLockScreenNotification(account: LegacyAccountDto): Notification {
        return notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconSendingMail)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(resourceProvider.sendingMailTitle())
            .build()
    }

    private fun createFetchingMailLockScreenNotification(account: LegacyAccountDto): Notification {
        return notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconCheckingMail)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(resourceProvider.checkingMailTitle())
            .build()
    }

    private val notificationManager: NotificationManagerCompat
        get() = notificationHelper.getNotificationManager()
}
