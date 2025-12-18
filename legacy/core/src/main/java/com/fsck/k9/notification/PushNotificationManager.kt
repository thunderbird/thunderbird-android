package com.fsck.k9.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import com.fsck.k9.CoreResourceProvider
import net.thunderbird.core.logging.Logger

private const val PUSH_INFO_ACTION = "app.k9mail.action.PUSH_INFO"
private const val TAG = "PushNotificationManager"

internal class PushNotificationManager(
    private val context: Context,
    private val resourceProvider: CoreResourceProvider,
    private val iconResourceProvider: NotificationIconResourceProvider,
    private val notificationChannelManager: NotificationChannelManager,
    private val notificationManager: NotificationManagerCompat,
    private val logger: Logger,
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
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            logger.error(TAG, e) { "Failed to post updated notification for $notificationId" }
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(context, notificationChannelManager.pushChannelId)
            .setSmallIcon(iconResourceProvider.pushNotificationIcon)
            .setContentTitle(resourceProvider.pushNotificationText(notificationState))
            .setContentText(getContentText())
            .setContentIntent(getContentIntent())
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
            .setLocalOnly(true)
            .setShowWhen(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun getContentIntent(): PendingIntent {
        val intent = if (notificationState == PushNotificationState.ALARM_PERMISSION_MISSING) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                error("ACTION_REQUEST_SCHEDULE_EXACT_ALARM is only available on API 31+")
            }

            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(PUSH_INFO_ACTION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                setPackage(context.packageName)
            }
        }

        return PendingIntentCompat.getActivity(context, 1, intent, 0, false)!!
    }

    private fun getContentText(): String {
        return if (notificationState == PushNotificationState.ALARM_PERMISSION_MISSING) {
            resourceProvider.pushNotificationGrantAlarmPermissionText()
        } else {
            resourceProvider.pushNotificationInfoText()
        }
    }
}

enum class PushNotificationState {
    INITIALIZING,
    LISTENING,
    WAIT_BACKGROUND_SYNC,
    WAIT_NETWORK,
    ALARM_PERMISSION_MISSING,
}
