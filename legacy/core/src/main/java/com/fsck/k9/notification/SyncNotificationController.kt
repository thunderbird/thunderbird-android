package com.fsck.k9.notification

import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import com.fsck.k9.mailstore.LocalFolder
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager

internal class SyncNotificationController(
    private val notificationHelper: NotificationHelper,
    private val actionBuilder: NotificationActionCreator,
    private val resourceProvider: NotificationResourceProvider,
    private val outboxFolderManager: OutboxFolderManager,
    private val iconResourceProvider: NotificationIconResourceProvider,
    private val accountManager: LegacyAccountManager,
    private val notificationIdRegistry: AccountNotificationIdRegistry,
) {
    fun showSendingNotification(accountId: AccountId) {
        val account = accountManager.findById(accountId)
            ?: throw IllegalArgumentException("Account not found")
        val accountName = account.profile.name
        val title = resourceProvider.sendingMailTitle()
        val tickerText = resourceProvider.sendingMailBody(accountName)

        val notificationId = getNotificationId(accountId)
        val outboxFolderId = outboxFolderManager
            .getOutboxFolderIdSync(accountId)
            .takeIf { it != -1L }
            ?: error("Outbox folder not configured")
        val showMessageListPendingIntent = actionBuilder.createViewFolderPendingIntent(accountId, outboxFolderId)

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(
                account.id,
                NotificationChannelManager.ChannelType.MISCELLANEOUS,
                account.messagesNotificationChannelVersion,
            )
            .setSmallIcon(resourceProvider.iconSendingMail)
            .setColor(account.profile.color)
            .setWhen(System.currentTimeMillis())
            .setOngoing(true)
            .setTicker(tickerText)
            .setContentTitle(title)
            .setContentText(accountName)
            .setContentIntent(showMessageListPendingIntent)
            .setPublicVersion(createSendingLockScreenNotification(account))

        notificationHelper.notify(notificationId, notificationBuilder.build())
    }

    fun clearSendingNotification(accountId: AccountId) {
        val notificationId = getNotificationId(accountId)
        notificationManager.cancel(notificationId)
    }

    private fun getNotificationId(accountId: AccountId): Int {
        return notificationIdRegistry.getOrAllocate(accountId, AccountNotificationKind.Sync)
    }

    fun showFetchingMailNotification(accountId: AccountId, folder: LocalFolder) {
        val account = accountManager.findById(accountId)
            ?: throw IllegalArgumentException("Account not found")
        val accountName = account.profile.name
        val folderId = folder.databaseId
        val folderName = folder.name
        val tickerText = resourceProvider.checkingMailTicker(accountName, folderName)
        val title = resourceProvider.checkingMailTitle()

        // TODO: Use format string from resources
        val text = accountName + resourceProvider.checkingMailSeparator() + folderName

        val notificationId = getNotificationId(accountId)
        val showMessageListPendingIntent = actionBuilder.createViewFolderPendingIntent(accountId, folderId)

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(
                account.id,
                NotificationChannelManager.ChannelType.MISCELLANEOUS,
                account.messagesNotificationChannelVersion,
            )
            .setSmallIcon(iconResourceProvider.pushNotificationIcon)
            .setColor(account.profile.color)
            .setWhen(System.currentTimeMillis())
            .setOngoing(true)
            .setTicker(tickerText)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(showMessageListPendingIntent)
            .setPublicVersion(createFetchingMailLockScreenNotification(account))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        notificationHelper.notify(notificationId, notificationBuilder.build())
    }

    fun showEmptyFetchingMailNotification(accountId: AccountId) {
        val account = accountManager.findById(accountId)
            ?: throw IllegalArgumentException("Account not found")
        val title = resourceProvider.checkingMailTitle()
        val text = account.profile.name
        val notificationId = getNotificationId(accountId)

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(
                account.id,
                NotificationChannelManager.ChannelType.MISCELLANEOUS,
                account.messagesNotificationChannelVersion,
            )
            .setSmallIcon(iconResourceProvider.pushNotificationIcon)
            .setColor(account.profile.color)
            .setWhen(System.currentTimeMillis())
            .setOngoing(true)
            .setContentTitle(title)
            .setContentText(text)
            .setPublicVersion(createFetchingMailLockScreenNotification(account))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        notificationHelper.notify(notificationId, notificationBuilder.build())
    }

    fun clearFetchingMailNotification(accountId: AccountId) {
        val notificationId = getNotificationId(accountId)
        notificationManager.cancel(notificationId)
    }

    private fun createSendingLockScreenNotification(account: LegacyAccount): Notification {
        return notificationHelper
            .createNotificationBuilder(
                account.id,
                NotificationChannelManager.ChannelType.MISCELLANEOUS,
                account.messagesNotificationChannelVersion,
            )
            .setSmallIcon(resourceProvider.iconSendingMail)
            .setColor(account.profile.color)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(resourceProvider.sendingMailTitle())
            .build()
    }

    private fun createFetchingMailLockScreenNotification(account: LegacyAccount): Notification {
        return notificationHelper
            .createNotificationBuilder(
                account.id,
                NotificationChannelManager.ChannelType.MISCELLANEOUS,
                account.messagesNotificationChannelVersion,
            )
            .setSmallIcon(resourceProvider.iconCheckingMail)
            .setColor(account.profile.color)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(resourceProvider.checkingMailTitle())
            .build()
    }

    private val notificationManager: NotificationManagerCompat
        get() = notificationHelper.getNotificationManager()
}
