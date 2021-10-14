package com.fsck.k9.notification

import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.Account

internal open class AuthenticationErrorNotificationController(
    private val notificationHelper: NotificationHelper,
    private val actionCreator: NotificationActionCreator,
    private val resourceProvider: NotificationResourceProvider
) {
    fun showAuthenticationErrorNotification(account: Account, incoming: Boolean) {
        val notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, incoming)
        val editServerSettingsPendingIntent = createContentIntent(account, incoming)
        val title = resourceProvider.authenticationErrorTitle()
        val text = resourceProvider.authenticationErrorBody(account.description)

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconWarning)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(editServerSettingsPendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ERROR)

        notificationHelper.configureNotification(
            builder = notificationBuilder,
            ringtone = null,
            vibrationPattern = null,
            ledColor = NotificationHelper.NOTIFICATION_LED_FAILURE_COLOR,
            ledSpeed = NotificationHelper.NOTIFICATION_LED_BLINK_FAST,
            ringAndVibrate = true
        )

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun clearAuthenticationErrorNotification(account: Account, incoming: Boolean) {
        val notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, incoming)
        notificationManager.cancel(notificationId)
    }

    protected open fun createContentIntent(account: Account, incoming: Boolean): PendingIntent {
        return if (incoming) {
            actionCreator.getEditIncomingServerSettingsIntent(account)
        } else {
            actionCreator.getEditOutgoingServerSettingsIntent(account)
        }
    }

    private val notificationManager: NotificationManagerCompat
        get() = notificationHelper.getNotificationManager()
}
