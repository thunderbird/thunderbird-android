package net.thunderbird.feature.notification.impl.receiver

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import net.thunderbird.core.common.provider.ContextProvider
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.content.InAppNotification
import org.jetbrains.annotations.VisibleForTesting

private const val TAG = "InAppNotificationNotifier"
internal actual inline fun <reified TContext> InAppNotificationNotifier(
    logger: Logger,
    contextProvider: ContextProvider<TContext>,
): InAppNotificationNotifier {
    require(TContext::class == Context::class) {
        "InAppNotificationNotifier expects an Android Context."
    }
    val context = contextProvider.context as Context
    return AndroidInAppNotificationNotifier(logger = logger, context = context)
}

@VisibleForTesting
internal class AndroidInAppNotificationNotifier(
    private val logger: Logger,
    context: Context,
) : InAppNotificationNotifier {
    private val context = context.applicationContext
    private var broadcastReceiver: InAppNotificationBroadcastReceiver? = null

    init {
        startBroadcast()
    }

    override fun show(id: NotificationId, notification: InAppNotification) {
        logger.debug(TAG) { "show() called with id = $id, notification = $notification" }
        val intent = Intent(POST_IN_APP_NOTIFICATION_ACTION).apply {
            setPackage(context.packageName)
            putExtra(IN_APP_NOTIFICATION_EXTRA, notification)
        }
        context.sendOrderedBroadcast(
            /* intent = */
            intent,
            /* receiverPermission = */
            POST_IN_APP_NOTIFICATION_PERMISSION,
        )
    }

    override fun dispose() {
        logger.debug(TAG) { "dispose() called" }
        context.unregisterReceiver(broadcastReceiver)
        broadcastReceiver = null
    }

    private fun startBroadcast() {
        logger.debug(TAG) { "startBroadcast() called" }
        val filter = IntentFilter(POST_IN_APP_NOTIFICATION_ACTION).apply {
            priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        }
        broadcastReceiver = InAppNotificationBroadcastReceiver()

        ContextCompat.registerReceiver(
            /* context = */
            context,
            /* receiver = */
            broadcastReceiver,
            /* filter = */
            filter,
            /* broadcastPermission = */
            POST_IN_APP_NOTIFICATION_PERMISSION,
            /* scheduler = */
            null,
            /* flags = */
            InAppNotificationBroadcastReceiver.FLAGS,

        )
    }
}
