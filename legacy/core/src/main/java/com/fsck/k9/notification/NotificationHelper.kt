package com.fsck.k9.notification

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import app.k9mail.legacy.di.DI
import com.fsck.k9.QuietTimeChecker
import com.fsck.k9.notification.NotificationChannelManager.ChannelType
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.notification.NotificationPreference

class NotificationHelper(
    private val context: Context,
    private val notificationManager: NotificationManagerCompat,
    private val notificationChannelManager: NotificationChannelManager,
    private val resourceProvider: NotificationResourceProvider,
    private val generalSettingsManager: GeneralSettingsManager,
) {
    fun getContext(): Context {
        return context
    }

    fun getNotificationManager(): NotificationManagerCompat {
        return notificationManager
    }

    fun createNotificationBuilder(account: LegacyAccount, channelType: ChannelType): NotificationCompat.Builder {
        val notificationChannel = notificationChannelManager.getChannelIdFor(account, channelType)
        return NotificationCompat.Builder(context, notificationChannel)
    }

    fun notify(account: LegacyAccount, notificationId: Int, notification: Notification) {
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // When importing settings from another device, we could end up with a NotificationChannel that references
            // a non-existing notification sound. In that case, we end up with a SecurityException with a message
            // similar to this:
            // UID 123 does not have permission to
            // content://media/external_primary/audio/media/42?title=Coins&canonical=1 [user 0]
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                e.message?.contains("does not have permission to") == true
            ) {
                Log.e(e, "Failed to create a notification for a new message")
                showNotifyErrorNotification(account)
            } else {
                throw e
            }
        }
    }

    private fun showNotifyErrorNotification(account: LegacyAccount) {
        val title = resourceProvider.notifyErrorTitle()
        val text = resourceProvider.notifyErrorText()

        val messagesNotificationChannelId = notificationChannelManager.getChannelIdFor(account, ChannelType.MESSAGES)
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_CHANNEL_ID, messagesNotificationChannelId)
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }

        val notificationSettingsPendingIntent =
            PendingIntentCompat.getActivity(context, account.accountNumber, intent, 0, false)

        val notification = createNotificationBuilder(account, ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconWarning)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(notificationSettingsPendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setErrorAppearance(generalSettingsManager = generalSettingsManager)
            .build()

        val notificationId = NotificationIds.getNewMailSummaryNotificationId(account)
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        internal const val NOTIFICATION_LED_ON_TIME = 500
        internal const val NOTIFICATION_LED_OFF_TIME = 2000
        internal const val NOTIFICATION_LED_FAST_ON_TIME = 100
        internal const val NOTIFICATION_LED_FAST_OFF_TIME = 100

        internal const val NOTIFICATION_LED_FAILURE_COLOR = 0xFFFF0000L.toInt()
    }
}

internal fun NotificationCompat.Builder.setErrorAppearance(
    generalSettingsManager: GeneralSettingsManager,
): NotificationCompat.Builder =
    apply {
        setSilent(true)

        if (!generalSettingsManager.getSettings().notification.isQuietTime) {
            setLights(
                NotificationHelper.NOTIFICATION_LED_FAILURE_COLOR,
                NotificationHelper.NOTIFICATION_LED_FAST_ON_TIME,
                NotificationHelper.NOTIFICATION_LED_FAST_OFF_TIME,
            )
        }
    }

internal fun NotificationCompat.Builder.setAppearance(
    silent: Boolean,
    appearance: NotificationAppearance,
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
                NotificationHelper.NOTIFICATION_LED_OFF_TIME,
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
internal val NotificationPreference.isQuietTime: Boolean
    get() {
        val clock = DI.get<Clock>()
        val quietTimeChecker = QuietTimeChecker(
            clock = clock,
            quietTimeStart = quietTimeStarts,
            quietTimeEnd = quietTimeEnds,
        )
        return quietTimeChecker.isQuietTime
    }
