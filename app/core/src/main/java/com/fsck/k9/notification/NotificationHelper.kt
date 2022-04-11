package com.fsck.k9.notification

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.Account
import com.fsck.k9.K9

class NotificationHelper(
    private val context: Context,
    private val notificationManager: NotificationManagerCompat,
    private val channelUtils: NotificationChannelManager
) {
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
        internal const val NOTIFICATION_LED_FAST_ON_TIME = 100
        internal const val NOTIFICATION_LED_FAST_OFF_TIME = 100

        internal const val NOTIFICATION_LED_FAILURE_COLOR = 0xFFFF0000L.toInt()
    }
}

internal fun NotificationCompat.Builder.setErrorAppearance(): NotificationCompat.Builder = apply {
    setSilent(true)

    if (!K9.isQuietTime) {
        setLights(
            NotificationHelper.NOTIFICATION_LED_FAILURE_COLOR,
            NotificationHelper.NOTIFICATION_LED_FAST_ON_TIME,
            NotificationHelper.NOTIFICATION_LED_FAST_OFF_TIME
        )
    }
}

internal fun NotificationCompat.Builder.setAppearance(
    silent: Boolean,
    appearance: NotificationAppearance
): NotificationCompat.Builder = apply {
    if (silent) {
        setSilent(true)
    } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
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
