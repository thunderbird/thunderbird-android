package net.thunderbird.feature.notification.impl.command

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.command.NotificationCommand.CommandOutcome.Failure
import net.thunderbird.feature.notification.api.command.NotificationCommand.CommandOutcome.Success
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier

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
    override suspend fun execute(): Outcome<Success<InAppNotification>, Failure<InAppNotification>> {
        return if (canExecuteCommand()) {
            notifier.show(id = NotificationId.Undefined, notification = notification)
            Outcome.success(Success(command = this))
        } else {
            Outcome.failure(Failure(command = this, throwable = Exception("Can't execute command.")))
        }
    }

    // TODO(#9392): Verify if the app is on foreground. IF it isn't, then should fail
    //  executing the command
    // TODO(#9420): If the app is on background and the severity is Fatal or Critical, we should
    //  let the command execute, but store it in a database instead of triggering the show notification logic.
    private fun canExecuteCommand(): Boolean = true
}
