package com.fsck.k9.notification

import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.helper.ExceptionHelper
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager

internal class SendFailedNotificationController(
    private val notificationHelper: NotificationHelper,
    private val actionBuilder: NotificationActionCreator,
    private val resourceProvider: NotificationResourceProvider,
    private val generalSettingsManager: GeneralSettingsManager,
    private val outboxFolderManager: OutboxFolderManager,
) {
    fun showSendFailedNotification(account: LegacyAccountDto, exception: Exception) {
        val title = resourceProvider.sendFailedTitle()
        val text = ExceptionHelper.getRootCauseMessage(exception)

        val notificationId = NotificationIds.getSendFailedNotificationId(account)

        val pendingIntent = outboxFolderManager.getOutboxFolderIdSync(account.id).let { outboxFolderId ->
            if (outboxFolderId != -1L) {
                actionBuilder.createViewFolderPendingIntent(account, outboxFolderId)
            } else {
                actionBuilder.createViewFolderListPendingIntent(account)
            }
        }

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconWarning)
            .setColor(account.chipColor)
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

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun clearSendFailedNotification(account: LegacyAccountDto) {
        val notificationId = NotificationIds.getSendFailedNotificationId(account)
        notificationManager.cancel(notificationId)
    }

    private fun createLockScreenNotification(account: LegacyAccountDto): Notification {
        return notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconWarning)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(resourceProvider.sendFailedTitle())
            .build()
    }

    private val notificationManager: NotificationManagerCompat
        get() = notificationHelper.getNotificationManager()
}
