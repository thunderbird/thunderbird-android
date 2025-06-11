package net.thunderbird.feature.notification.impl.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Parcelable
import androidx.core.content.ContextCompat
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.notification.api.content.InAppNotification
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal const val POST_IN_APP_NOTIFICATION_PERMISSION =
    "net.thunderbird.permission.POST_IN_APP_NOTIFICATION_PERMISSION"
internal const val POST_IN_APP_NOTIFICATION_ACTION =
    "net.thunderbird.feature.notification.receiver.POST_IN_APP_NOTIFICATION_ACTION"
internal const val IN_APP_NOTIFICATION_EXTRA =
    "net.thunderbird.feature.notification.receiver.POST_IN_APP_NOTIFICATION_EXTRA"
private const val TAG = "InAppNotificationBroadcastReceiver"

class InAppNotificationBroadcastReceiver : BroadcastReceiver(), KoinComponent {
    private val logger: Logger by inject<Logger>()

    override fun onReceive(context: Context, intent: Intent) {
        logger.debug(TAG) { "onReceive() called with: context = $context, intent = $intent" }
        val intent = intent.takeIf { it.action == POST_IN_APP_NOTIFICATION_ACTION } ?: return

        val notification = intent.getParcelable<InAppNotification>(IN_APP_NOTIFICATION_EXTRA)

        logger.debug(TAG) { "Received In-app notification: $notification" }
    }

    companion object {
        const val FLAGS = ContextCompat.RECEIVER_NOT_EXPORTED
    }

    private inline fun <reified TParcelable : Parcelable> Intent.getParcelable(name: String): TParcelable? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(name, TParcelable::class.java)
        } else {
            getParcelableExtra<TParcelable>(name)
        }
}
