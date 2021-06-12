package com.fsck.k9.notification

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.CoreResourceProvider

internal class PushNotificationManager(
    private val context: Context,
    private val resourceProvider: CoreResourceProvider,
    private val notificationChannelManager: NotificationChannelManager,
    private val notificationManager: NotificationManagerCompat
) {
    val notificationId = NotificationIds.PUSH_NOTIFICATION_ID

    @get:Synchronized
    @set:Synchronized
    var notificationState = PushNotificationState.INITIALIZING
        set(value) {
            field = value

            if (isForegroundServiceStarted) {
                updateNotification()
            }
        }

    private var isForegroundServiceStarted = false

    @Synchronized
    fun createForegroundNotification(): Notification {
        isForegroundServiceStarted = true
        return createNotification()
    }

    @Synchronized
    fun setForegroundServiceStopped() {
        isForegroundServiceStarted = false
    }

    private fun updateNotification() {
        val notification = createNotification()
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(context, notificationChannelManager.pushChannelId)
            .setSmallIcon(resourceProvider.iconPushNotification)
            .setContentTitle(resourceProvider.pushNotificationText(notificationState))
            .setOngoing(true)
            .setNotificationSilent()
            .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
            .setLocalOnly(true)
            .setShowWhen(false)
            .build()
    }
}

enum class PushNotificationState {
    INITIALIZING,
    LISTENING,
    WAIT_BACKGROUND_SYNC,
    WAIT_NETWORK
}
