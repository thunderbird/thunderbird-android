package net.thunderbird.feature.notification.impl.ui.action

import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.impl.intent.action.NotificationActionIntentCreator

interface NotificationActionCreator<TNotification : Notification> {
    suspend fun create(notification: TNotification, action: NotificationAction): AndroidNotificationAction

    enum class TypeQualifier { System, InApp }
}

private const val TAG = "DefaultSystemNotificationActionCreator"

internal class DefaultSystemNotificationActionCreator(
    private val logger: Logger,
    private val actionIntentCreators: List<NotificationActionIntentCreator<NotificationAction>>,
) : NotificationActionCreator<SystemNotification> {
    override suspend fun create(
        notification: SystemNotification,
        action: NotificationAction,
    ): AndroidNotificationAction {
        logger.debug(TAG) { "create() called with: notification = $notification, action = $action" }
        val intent = actionIntentCreators
            .first { it.accept(action) }
            .create(action)

        return AndroidNotificationAction(
            icon = action.icon.resolve(notification),
            title = action.resolveTitle(),
            pendingIntent = intent,
        )
    }
}
