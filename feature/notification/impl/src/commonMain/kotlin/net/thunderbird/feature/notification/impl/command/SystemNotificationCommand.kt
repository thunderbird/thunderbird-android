package net.thunderbird.feature.notification.impl.command

import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier

/**
 * Command for displaying system notifications.
 *
 * @param notification The system notification to display.
 * @param notifier The notifier responsible for displaying the notification.
 */
internal class SystemNotificationCommand(
    private val logger: Logger,
    notification: SystemNotification,
    notifier: NotificationNotifier<SystemNotification>,
) : NotificationCommand<SystemNotification>(notification, notifier) {
    override suspend fun execute(): Outcome<Success<SystemNotification>, Failure<SystemNotification>> {
        logger.debug {
            "TODO: Implementation on GitHub Issue #9245. Notification = $notification."
        }
        return Outcome.success(data = Success(command = this))
    }
}
