package net.thunderbird.feature.notification.command

import net.thunderbird.feature.notification.content.InAppNotification
import net.thunderbird.feature.notification.receiver.NotificationNotifier

/**
 * A command that handles in-app notifications.
 *
 * This class is responsible for executing the logic associated with displaying an in-app notification.
 *
 * @param notification The [InAppNotification] to be handled.
 * @param notifier The [NotificationNotifier] responsible for actually displaying the notification.
 */
internal class InAppNotificationCommand(
    notification: InAppNotification,
    notifier: NotificationNotifier<InAppNotification>,
) : NotificationCommand<InAppNotification>(notification, notifier) {
    override fun execute(): CommandResult {
        TODO("Implementation on GitHub Issue #9245")
    }
}
