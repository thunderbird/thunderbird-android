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
            return
        }

        if (ringAndVibrate) {
            if (ringtone != null && !TextUtils.isEmpty(ringtone)) {
                builder.setSound(Uri.parse(ringtone))
            }

            if (vibrationPattern != null) {
                builder.setVibrate(vibrationPattern)
            }
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

    fun getAccountName(account: Account): String {
        val accountDescription = account.description
        return if (TextUtils.isEmpty(accountDescription)) account.email else accountDescription
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
        return NotificationCompat.Builder(context,
                channelUtils.getChannelIdFor(account, channelType))
    }

    companion object {
        private const val NOTIFICATION_LED_ON_TIME = 500
        private const val NOTIFICATION_LED_OFF_TIME = 2000
        private const val NOTIFICATION_LED_FAST_ON_TIME = 100
        private const val NOTIFICATION_LED_FAST_OFF_TIME = 100

        internal const val NOTIFICATION_LED_BLINK_SLOW = 0
        internal const val NOTIFICATION_LED_BLINK_FAST = 1
        internal const val NOTIFICATION_LED_FAILURE_COLOR = -0x10000
    }
}
