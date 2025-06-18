package net.thunderbird.feature.notification.impl.command

import net.thunderbird.feature.notification.api.NotificationIdFactory
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier

/**
 * A factory for creating a set of notification commands based on a given notification.
 */
internal class NotificationCommandFactory(
    private val notificationIdFactory: NotificationIdFactory,
    private val systemNotificationNotifier: NotificationNotifier<SystemNotification>,
    private val inAppNotificationNotifier: NotificationNotifier<InAppNotification>,
) {
    /**
     * Creates a set of [NotificationCommand]s for the given [notification].
     *
     * The commands are returned in a [LinkedHashSet] to preserve the order in which they should be executed.
     *
     * @param notification The notification for which to create commands.
     * @return A set of notification commands.
     */
    fun create(notification: Notification): LinkedHashSet<NotificationCommand<out Notification>> {
        val commands = linkedSetOf<NotificationCommand<out Notification>>()

        if (notification is SystemNotification) {
            commands.add(
                SystemNotificationCommand(
                    notificationIdFactory = notificationIdFactory,
                    notification = notification,
                    notifier = systemNotificationNotifier,
                ),
            )
        }

        if (notification is InAppNotification) {
            commands.add(
                InAppNotificationCommand(
                    notification = notification,
                    notifier = inAppNotificationNotifier,
                ),
            )
        }

        return commands
    }
}
