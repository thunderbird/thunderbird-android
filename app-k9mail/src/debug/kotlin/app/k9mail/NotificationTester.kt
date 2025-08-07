package app.k9mail

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import com.fsck.k9.notification.NotificationChannelManager

object NotificationTester {
    fun showTestPushNotification(
        context: Context,
        iconProvider: Lazy<NotificationIconResourceProvider>,
        channelManager: Lazy<NotificationChannelManager>,
    ) {
        val n = NotificationCompat.Builder(context, channelManager.value.pushChannelId)
            .setSmallIcon(iconProvider.value.pushNotificationIcon)
            .setContentTitle("🔔 Debug Push")
            .setContentText("This is a test using the app icon")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context)
            .notify(0xDEAD, n)
    }
}
