package net.thunderbird.feature.notification.impl.command

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.NotificationIdFactory
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.command.NotificationCommand.CommandOutcome.Failure
import net.thunderbird.feature.notification.api.command.NotificationCommand.CommandOutcome.Success
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier

/**
 * Command for displaying system notifications.
 *
 * @param notification The system notification to display.
 * @param notifier The notifier responsible for displaying the notification.
 */
internal class SystemNotificationCommand(
    private val notificationIdFactory: NotificationIdFactory,
    notification: SystemNotification,
    notifier: NotificationNotifier<SystemNotification>,
) : NotificationCommand<SystemNotification>(notification, notifier) {
    override suspend fun execute(): Outcome<Success<SystemNotification>, Failure<SystemNotification>> {
        return if (canExecuteCommand()) {
            notifier.show(
                id = notificationIdFactory.next(notification.accountNumber),
                notification = notification,
            )
            Outcome.success(Success(command = this))
        } else {
            Outcome.failure(Failure(command = this, throwable = Exception("Can't execute command.")))
        }
    }

    private fun canExecuteCommand(): Boolean {
        val isBackgrounded = false // TODO(#9391): Verify if the app is backgrounded.
        val shouldAlwaysShow = when (notification.severity) {
            NotificationSeverity.Fatal, NotificationSeverity.Critical -> true
            else -> false
        }

        return when {
            shouldAlwaysShow -> true
            isBackgrounded -> true
            notification !is InAppNotification -> true
            else -> false
        }
    }
}
