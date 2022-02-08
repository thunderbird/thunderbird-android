package com.fsck.k9.notification

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.Account
import com.fsck.k9.K9

class NotificationHelper(
    private val context: Context,
    private val notificationManager: NotificationManagerCompat,
    private val channelUtils: NotificationChannelManager
) {
    fun configureNotification(
        builder: NotificationCompat.Builder,
        ringtone: String?,
        vibrationPattern: LongArray?,
        ledColor: Int?,
        ledSpeed: Int,
        ringAndVibrate: Boolean
    ) {

        if (K9.isQuietTime) {
            builder.setNotificationSilent()
            return
        }

        if (ringAndVibrate) {
            if (ringtone != null && !TextUtils.isEmpty(ringtone)) {
                builder.setSound(Uri.parse(ringtone))
            }

            if (vibrationPattern != null) {
                builder.setVibrate(vibrationPattern)
            }
        } else {
            builder.setNotificationSilent()
        }

        if (ledColor != null) {
            val ledOnMS: Int
            val ledOffMS: Int
            if (ledSpeed == NOTIFICATION_LED_BLINK_SLOW) {
                ledOnMS = NOTIFICATION_LED_ON_TIME
                ledOffMS = NOTIFICATION_LED_OFF_TIME
            } else {
                ledOnMS = NOTIFICATION_LED_FAST_ON_TIME
                ledOffMS = NOTIFICATION_LED_FAST_OFF_TIME
            }

            builder.setLights(ledColor, ledOnMS, ledOffMS)
        }
    }

    fun getContext(): Context {
        return context
    }

    fun getNotificationManager(): NotificationManagerCompat {
        return notificationManager
    }

    fun createNotificationBuilder(
        account: Account,
        channelType: NotificationChannelManager.ChannelType
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(
            context,
            channelUtils.getChannelIdFor(account, channelType)
        )
    }

    companion object {
        internal const val NOTIFICATION_LED_ON_TIME = 500
        internal const val NOTIFICATION_LED_OFF_TIME = 2000
        private const val NOTIFICATION_LED_FAST_ON_TIME = 100
        private const val NOTIFICATION_LED_FAST_OFF_TIME = 100

        internal const val NOTIFICATION_LED_BLINK_SLOW = 0
        internal const val NOTIFICATION_LED_BLINK_FAST = 1
        internal const val NOTIFICATION_LED_FAILURE_COLOR = -0x10000
    }
}

internal fun NotificationCompat.Builder.setAppearance(
    silent: Boolean,
    appearance: NotificationAppearance
): NotificationCompat.Builder = apply {
    if (silent) {
        setSilent(true)
    } else {
        if (!appearance.ringtone.isNullOrEmpty()) {
            setSound(Uri.parse(appearance.ringtone))
        }

        if (appearance.vibrationPattern != null) {
            setVibrate(appearance.vibrationPattern)
        }

        if (appearance.ledColor != null) {
            setLights(
                appearance.ledColor,
                NotificationHelper.NOTIFICATION_LED_ON_TIME,
                NotificationHelper.NOTIFICATION_LED_OFF_TIME
            )
        }
    }
}
