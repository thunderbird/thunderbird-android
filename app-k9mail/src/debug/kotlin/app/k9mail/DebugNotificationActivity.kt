package app.k9mail

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import com.fsck.k9.notification.NotificationChannelManager
import org.koin.java.KoinJavaComponent.inject

class DebugNotificationActivity : Activity() {

    private val iconProvider: Lazy<NotificationIconResourceProvider> =
        inject(NotificationIconResourceProvider::class.java)
    private val channelManager: Lazy<NotificationChannelManager> =
        inject(NotificationChannelManager::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 128, 64, 64)
        }

        val title = TextView(this).apply {
            text = "🔔 Debug Push Notification"
            textSize = 22f
        }

        val button = Button(this).apply {
            text = "Trigger Notification"
            setOnClickListener {
                NotificationTester.showTestPushNotification(
                    context = this@DebugNotificationActivity,
                    iconProvider = iconProvider,
                    channelManager = channelManager,
                )
            }
        }

        layout.addView(title)
        layout.addView(button)

        setContentView(layout)
    }
}
