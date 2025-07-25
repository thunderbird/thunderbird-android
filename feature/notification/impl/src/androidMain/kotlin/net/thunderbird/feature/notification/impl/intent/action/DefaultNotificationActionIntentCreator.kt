package net.thunderbird.feature.notification.impl.intent.action

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.PendingIntentCompat
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.notification.api.ui.action.NotificationAction

private const val TAG = "DefaultNotificationActionIntentCreator"

/**
 * A default implementation of [NotificationActionIntentCreator] that creates a [PendingIntent]
 * to launch the application when a notification action is triggered.
 *
 * This creator accepts any [NotificationAction] and always attempts to create a launch intent
 * for the current application.
 *
 * @property logger The logger instance for logging debug messages.
 * @property applicationContext The application context used to access system services like PackageManager.
 */
internal class DefaultNotificationActionIntentCreator(
    private val logger: Logger,
    private val applicationContext: Context,
) : NotificationActionIntentCreator<NotificationAction> {
    override fun accept(action: NotificationAction): Boolean = true

    override fun create(action: NotificationAction): PendingIntent? {
        logger.debug(TAG) { "create() called with: action = $action" }
        val packageManager = applicationContext.packageManager
        val launchIntent = requireNotNull(
            packageManager.getLaunchIntentForPackage(applicationContext.packageName),
        ) {
            "Could not retrieve the launch intent from ${applicationContext.packageName}"
        }

        return PendingIntentCompat.getActivity(
            /* context = */
            applicationContext,
            /* requestCode = */
            1,
            /* intent = */
            launchIntent,
            /* flags = */
            0,
            /* isMutable = */
            false,
        )
    }
}
