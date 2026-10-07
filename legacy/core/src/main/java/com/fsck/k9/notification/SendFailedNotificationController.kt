package com.fsck.k9.notification

import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.exception.rootCauseMessage
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager

internal class SendFailedNotificationController(
    private val notificationHelper: NotificationHelper,
    private val actionBuilder: NotificationActionCreator,
    private val resourceProvider: NotificationResourceProvider,
    private val generalSettingsManager: GeneralSettingsManager,
    private val outboxFolderManager: OutboxFolderManager,
    private val accountManager: LegacyAccountManager,
    private val notificationIdRegistry: AccountNotificationIdRegistry,
) {
    fun showSendFailedNotification(accountId: AccountId, exception: Exception) {
        val account = accountManager.findById(accountId)
            ?: throw IllegalStateException("Account not found for id $accountId")
        val title = resourceProvider.sendFailedTitle()
        val text = exception.rootCauseMessage.orEmpty()

        val notificationId = getNotificationId(accountId)

        val pendingIntent = outboxFolderManager.getOutboxFolderIdSync(accountId).let { outboxFolderId ->
            if (outboxFolderId != -1L) {
                actionBuilder.createViewFolderPendingIntent(accountId, outboxFolderId)
            } else {
                actionBuilder.createViewFolderListPendingIntent(accountId)
            }
        }

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(
                account.id,
                NotificationChannelManager.ChannelType.MISCELLANEOUS,
                account.messagesNotificationChannelVersion,
            )
            .setSmallIcon(resourceProvider.iconWarning)
            .setColor(account.profile.color)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPublicVersion(createLockScreenNotification(account))
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setErrorAppearance(generalSettingsManager = generalSettingsManager)

        notificationHelper.notify(notificationId, notificationBuilder.build())
    }

    fun clearSendFailedNotification(accountId: AccountId) {
        val notificationId = getNotificationId(accountId)
        notificationManager.cancel(notificationId)
    }

    private fun getNotificationId(accountId: AccountId): Int {
        return notificationIdRegistry.getOrAllocate(accountId, AccountNotificationKind.SendFailed)
    }

    private fun createLockScreenNotification(account: LegacyAccount): Notification {
        return notificationHelper
            .createNotificationBuilder(
                account.id, NotificationChannelManager.ChannelType.MISCELLANEOUS,
                account.messagesNotificationChannelVersion,
            )
            .setSmallIcon(resourceProvider.iconWarning)
            .setColor(account.profile.color)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(resourceProvider.sendFailedTitle())
            .build()
    }

    private val notificationManager: NotificationManagerCompat
        get() = notificationHelper.getNotificationManager()
}
