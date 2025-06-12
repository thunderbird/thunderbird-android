package net.thunderbird.feature.notification.command

import net.thunderbird.feature.notification.content.SystemNotification
import net.thunderbird.feature.notification.receiver.NotificationNotifier

/**
 * Command for displaying system notifications.
 *
 * @param notification The system notification to display.
 * @param notifier The notifier responsible for displaying the notification.
 */
internal class SystemNotificationCommand(
    notification: SystemNotification,
    notifier: NotificationNotifier<SystemNotification>,
) : NotificationCommand<SystemNotification>(notification, notifier) {
    override fun execute(): CommandResult {
        TODO("Implementation on GitHub Issue #9245")
    }
}
