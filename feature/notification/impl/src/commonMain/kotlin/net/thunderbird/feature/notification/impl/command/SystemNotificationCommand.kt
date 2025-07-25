package net.thunderbird.feature.notification.impl.command

import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.command.NotificationCommandException
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier

private const val TAG = "SystemNotificationCommand"

/**
 * Command for displaying system notifications.
 *
 * @param notification The system notification to display.
 * @param notifier The notifier responsible for displaying the notification.
 */
internal class SystemNotificationCommand(
    private val logger: Logger,
    private val featureFlagProvider: FeatureFlagProvider,
    private val notificationRegistry: NotificationRegistry,
    notification: SystemNotification,
    notifier: NotificationNotifier<SystemNotification>,
    private val isAppInBackground: () -> Boolean = {
        // TODO(#9391): Verify if the app is backgrounded.
        false
    },
) : NotificationCommand<SystemNotification>(notification, notifier) {

    private val isFeatureFlagEnabled: Boolean
        get() = featureFlagProvider
            .provide(FeatureFlagKey.UseNotificationSenderForSystemNotifications) == FeatureFlagResult.Enabled

    override suspend fun execute(): Outcome<Success<SystemNotification>, Failure<SystemNotification>> {
        logger.debug(TAG) { "execute() called" }
        return when {
            isFeatureFlagEnabled.not() ->
                Outcome.failure(
                    error = Failure(
                        command = this,
                        throwable = NotificationCommandException(
                            message = "${FeatureFlagKey.UseNotificationSenderForSystemNotifications.key} feature flag" +
                                "is not enabled",
                        ),
                    ),
                )

            canExecuteCommand() -> {
                notifier.show(
                    id = notificationRegistry.register(notification),
                    notification = notification,
                )
                Outcome.success(Success(command = this))
            }

            else -> {
                Outcome.failure(
                    error = Failure(
                        command = this,
                        throwable = NotificationCommandException("Can't execute command."),
                    ),
                )
            }
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
