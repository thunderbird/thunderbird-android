package app.k9mail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import com.fsck.k9.notification.NotificationChannelManager
import org.koin.java.KoinJavaComponent.inject

class DebugNotificationReceiver : BroadcastReceiver() {
    private val iconProvider: Lazy<NotificationIconResourceProvider> =
        inject(NotificationIconResourceProvider::class.java)
    private val channelManager: Lazy<NotificationChannelManager> =
        inject(NotificationChannelManager::class.java)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("DebugNotifReceiver", "Received debug notification broadcast")
        NotificationTester.showTestPushNotification(
            context,
            iconProvider,
            channelManager,
        )
    }
}
