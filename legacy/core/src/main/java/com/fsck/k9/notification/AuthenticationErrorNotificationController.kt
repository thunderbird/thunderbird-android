package com.fsck.k9.notification

import android.app.Notification
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.k9mail.legacy.account.LegacyAccount

internal open class AuthenticationErrorNotificationController(
    private val notificationHelper: NotificationHelper,
    private val actionCreator: NotificationActionCreator,
    private val resourceProvider: NotificationResourceProvider,
) {
    fun showAuthenticationErrorNotification(account: LegacyAccount, incoming: Boolean) {
        val notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, incoming)
        val editServerSettingsPendingIntent = createContentIntent(account, incoming)
        val title = resourceProvider.authenticationErrorTitle()
        val text = resourceProvider.authenticationErrorBody(account.displayName)

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconWarning)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(editServerSettingsPendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPublicVersion(createLockScreenNotification(account))
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setErrorAppearance()

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun clearAuthenticationErrorNotification(account: LegacyAccount, incoming: Boolean) {
        val notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, incoming)
        notificationManager.cancel(notificationId)
    }

    protected open fun createContentIntent(account: LegacyAccount, incoming: Boolean): PendingIntent {
        return if (incoming) {
            actionCreator.getEditIncomingServerSettingsIntent(account)
        } else {
            actionCreator.getEditOutgoingServerSettingsIntent(account)
        }
    }

    private fun createLockScreenNotification(account: LegacyAccount): Notification {
        return notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconWarning)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(resourceProvider.authenticationErrorTitle())
            .build()
    }

    private val notificationManager: NotificationManagerCompat
        get() = notificationHelper.getNotificationManager()
}
