package app.k9mail

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import com.fsck.k9.notification.NotificationChannelManager
import org.koin.java.KoinJavaComponent.inject

class DebugNotificationActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("DebugNotifActivity", "onCreate called")

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
                try {
                    val iconProvider: Lazy<NotificationIconResourceProvider> =
                        inject(NotificationIconResourceProvider::class.java)
                    val channelManager: Lazy<NotificationChannelManager> =
                        inject(NotificationChannelManager::class.java)

                    NotificationTester.showTestPushNotification(
                        context = this@DebugNotificationActivity,
                        iconProvider = iconProvider,
                        channelManager = channelManager,
                    )
                    Toast.makeText(this@DebugNotificationActivity, "Notification sent!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("DebugNotifActivity", "Failed to inject dependencies", e)
                    Toast.makeText(this@DebugNotificationActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        layout.addView(title)
        layout.addView(button)

        setContentView(layout)

        Log.d("DebugNotifActivity", "Activity setup complete")
    }
}
