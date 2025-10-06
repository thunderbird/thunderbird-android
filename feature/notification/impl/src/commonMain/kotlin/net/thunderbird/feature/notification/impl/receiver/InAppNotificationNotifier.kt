package net.thunderbird.feature.notification.impl.receiver

import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.receiver.InAppNotificationEvent
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier

private const val TAG = "InAppNotificationNotifier"

/**
 * This notifier is responsible for taking a [InAppNotification] data object and
 * presenting it to the user in a suitable way.
 */
internal class InAppNotificationNotifier(
    private val logger: Logger,
    private val notificationRegistry: NotificationRegistry,
    private val inAppNotificationEventBus: InAppNotificationEventBus,
) : NotificationNotifier<InAppNotification> {

    override suspend fun show(id: NotificationId, notification: InAppNotification) {
        logger.debug(TAG) { "show() called with: id = $id, notification = $notification" }
        if (notificationRegistry.registrar.containsKey(id)) {
            inAppNotificationEventBus.publish(
                event = InAppNotificationEvent.Show(notification),
            )
        }
    }

    override fun dispose() {
        logger.debug(TAG) { "dispose() called" }
    }
}
