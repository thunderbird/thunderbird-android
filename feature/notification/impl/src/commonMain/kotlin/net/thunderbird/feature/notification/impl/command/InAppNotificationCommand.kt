package net.thunderbird.feature.notification.impl.command

import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.command.NotificationCommandException
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier

private const val TAG = "InAppNotificationCommand"

/**
 * A command that handles in-app notifications.
 *
 * This class is responsible for executing the logic associated with displaying an in-app notification.
 *
 * @param notification The [InAppNotification] to be handled.
 * @param notifier The [NotificationNotifier] responsible for actually displaying the notification.
 */
internal class InAppNotificationCommand(
    private val logger: Logger,
    private val featureFlagProvider: FeatureFlagProvider,
    private val notificationRegistry: NotificationRegistry,
    notification: InAppNotification,
    notifier: NotificationNotifier<InAppNotification>,
) : NotificationCommand<InAppNotification>(notification, notifier) {
    private val isFeatureFlagEnabled: Boolean
        get() = featureFlagProvider
            .provide(FeatureFlagKey.DisplayInAppNotifications) == FeatureFlagResult.Enabled

    override suspend fun execute(): Outcome<Success<InAppNotification>, Failure<InAppNotification>> {
        logger.debug(TAG) { "execute() called with: notification = $notification" }
        return when {
            isFeatureFlagEnabled.not() ->
                Outcome.failure(
                    error = Failure(
                        command = this,
                        throwable = NotificationCommandException(
                            message = "${FeatureFlagKey.DisplayInAppNotifications.key} feature flag is not enabled",
                        ),
                    ),
                )

            canExecuteCommand() -> {
                notifier.show(id = notificationRegistry.register(notification), notification = notification)
                Outcome.success(Success(command = this))
            }

            else -> {
                Outcome.failure(Failure(command = this, throwable = Exception("Can't execute command.")))
            }
        }
    }

    // TODO(#9392): Verify if the app is on foreground. IF it isn't, then should fail
    //  executing the command
    // TODO(#9420): If the app is on background and the severity is Fatal or Critical, we should
    //  let the command execute, but store it in a database instead of triggering the show notification logic.
    private fun canExecuteCommand(): Boolean = true
}
