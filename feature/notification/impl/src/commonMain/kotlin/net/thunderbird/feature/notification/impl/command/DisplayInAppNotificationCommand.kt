package net.thunderbird.feature.notification.impl.command

import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.command.outcome.CommandExecutionFailed
import net.thunderbird.feature.notification.api.command.outcome.NotificationCommandOutcome
import net.thunderbird.feature.notification.api.command.outcome.Success
import net.thunderbird.feature.notification.api.command.outcome.UnsupportedCommand
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier

private const val TAG = "DisplayInAppNotificationCommand"

/**
 * A command that handles in-app notifications.
 *
 * This class is responsible for executing the logic associated with displaying an in-app notification.
 *
 * @param notification The [InAppNotification] to be handled.
 * @param notifier The [NotificationNotifier] responsible for actually displaying the notification.
 */
internal class DisplayInAppNotificationCommand(
    private val logger: Logger,
    private val featureFlagProvider: FeatureFlagProvider,
    notification: InAppNotification,
    notifier: NotificationNotifier<InAppNotification>,
) : NotificationCommand<InAppNotification>(notification, notifier) {
    override suspend fun execute(): NotificationCommandOutcome<InAppNotification> {
        logger.debug(TAG) { "execute() called with: notification = $notification" }
        return when {
            featureFlagProvider.provide(FeatureFlagKey.DisplayInAppNotifications).isDisabledOrUnavailable() ->
                Outcome.failure(
                    error = UnsupportedCommand(
                        command = this,
                        reason = UnsupportedCommand.Reason.FeatureFlagDisabled(
                            key = FeatureFlagKey.DisplayInAppNotifications,
                        ),
                    ),
                )

            canExecuteCommand() -> {
                val id = notifier.show(notification = notification)
                Outcome.success(Success(notificationId = id, command = this))
            }

            else -> Outcome.failure(CommandExecutionFailed(command = this))
        }
    }

    // TODO(#9392): Verify if the app is on foreground. IF it isn't, then should fail
    //  executing the command
    // TODO(#9420): If the app is on background and the severity is Fatal or Critical, we should
    //  let the command execute, but store it in a database instead of triggering the show notification logic.
    private fun canExecuteCommand(): Boolean = true
}
