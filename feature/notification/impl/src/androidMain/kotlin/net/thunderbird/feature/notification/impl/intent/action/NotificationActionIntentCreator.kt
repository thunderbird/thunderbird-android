package net.thunderbird.feature.notification.impl.intent.action

import android.app.PendingIntent
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.notification.api.ui.action.NotificationAction

internal interface NotificationActionIntentCreator<in TNotificationAction : NotificationAction> {
    fun accept(action: NotificationAction): Boolean
    fun create(action: TNotificationAction): PendingIntent?

    object TypeQualifier
}

private const val TAG = "DefaultNotificationActionIntentCreator"
internal class DefaultNotificationActionIntentCreator(
    private val logger: Logger,
) : NotificationActionIntentCreator<NotificationAction> {
    override fun accept(action: NotificationAction): Boolean = true

    override fun create(action: NotificationAction): PendingIntent? {
        logger.debug(TAG) { "create() called with: action = $action" }
        return null
    }
}
