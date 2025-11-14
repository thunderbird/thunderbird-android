package net.thunderbird.feature.notification.impl.command

import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.command.outcome.CommandExecutionFailed
import net.thunderbird.feature.notification.api.command.outcome.NotificationCommandOutcome
import net.thunderbird.feature.notification.api.command.outcome.Success
import net.thunderbird.feature.notification.api.command.outcome.UnsupportedCommand
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier

private const val TAG = "DisplaySystemNotificationCommand"

/**
 * Command for displaying system notifications.
 *
 * @param notification The system notification to display.
 * @param notifier The notifier responsible for displaying the notification.
 */
internal class DisplaySystemNotificationCommand(
    private val logger: Logger,
    private val featureFlagProvider: FeatureFlagProvider,
    notification: SystemNotification,
    notifier: NotificationNotifier<SystemNotification>,
    private val isAppInBackground: () -> Boolean = {
        // TODO(#9391): Verify if the app is backgrounded.
        false
    },
) : NotificationCommand<SystemNotification>(notification, notifier) {
    override suspend fun execute(): NotificationCommandOutcome<SystemNotification> {
        logger.debug(TAG) { "execute() called with notification = $notification" }
        return when {
            featureFlagProvider
                .provide(FeatureFlagKey.UseNotificationSenderForSystemNotifications)
                .isDisabledOrUnavailable() -> Outcome.failure(
                error = UnsupportedCommand(
                    command = this,
                    reason = UnsupportedCommand.Reason.FeatureFlagDisabled(
                        key = FeatureFlagKey.UseNotificationSenderForSystemNotifications,
                    ),
                ),
            )

            canExecuteCommand() -> {
                val id = notifier.show(notification = notification)
                Outcome.success(Success(notificationId = id, command = this))
            }

            else -> Outcome.failure(error = CommandExecutionFailed(command = this))
        }
    }

    private fun canExecuteCommand(): Boolean {
        val shouldAlwaysShow = when (notification.severity) {
            NotificationSeverity.Fatal, NotificationSeverity.Critical -> true
            else -> false
        }

        return when {
            shouldAlwaysShow -> true
            isAppInBackground() -> true
            notification !is InAppNotification -> true
            else -> false
        }
    }
}
