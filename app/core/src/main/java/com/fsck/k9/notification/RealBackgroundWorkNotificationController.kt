package com.fsck.k9.notification

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat

class RealBackgroundWorkNotificationController(
    private val context: Context,
    private val resourceProvider: NotificationResourceProvider,
    private val notificationChannelManager: NotificationChannelManager,
) : BackgroundWorkNotificationController {
    override val notificationId = NotificationIds.BACKGROUND_WORK_NOTIFICATION_ID

    override fun createNotification(text: String): Notification {
        val notificationChannel = notificationChannelManager.miscellaneousChannelId

        return NotificationCompat.Builder(context, notificationChannel)
            .setSmallIcon(resourceProvider.iconBackgroundWorkNotification)
            .setContentTitle(text)
            .setOngoing(true)
            .setNotificationSilent()
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
            .setLocalOnly(true)
            .setShowWhen(false)
            .build()
    }
}
