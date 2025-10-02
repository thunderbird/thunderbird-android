package net.thunderbird.feature.notification.impl.command

import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.logging.LogTag
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.command.NotificationCommandException
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier

/**
 * A command that dismisses a notification.
 *
 * This command will only be executed if the [featureFlagKey] is enabled.
 *
 * @param TNotification The type of notification to dismiss.
 * @property logTag The log tag to use for logging.
 * @property logger The logger to use for logging.
 * @property featureFlagProvider The provider for feature flags.
 * @property notificationRegistry The registry of notifications.
 * @param notification The notification to dismiss.
 * @param notifier The notifier to use to dismiss the notification.
 */
sealed class DismissNotificationCommand<TNotification : Notification>(
    private val logTag: LogTag,
    private val logger: Logger,
    private val featureFlagProvider: FeatureFlagProvider,
    private val notificationRegistry: NotificationRegistry,
    notification: TNotification,
    notifier: NotificationNotifier<TNotification>,
) : NotificationCommand<TNotification>(
    notification = notification,
    notifier = notifier,
) {
    abstract val featureFlagKey: FeatureFlagKey

    override suspend fun execute(): Outcome<Success<TNotification>, Failure<TNotification>> {
        logger.verbose(logTag) { "execute() called with: notification = $notification" }
        return when {
            featureFlagProvider.provide(featureFlagKey).isDisabledOrUnavailable() ->
                Outcome.failure(
                    error = Failure(
                        command = this,
                        throwable = NotificationCommandException(
                            message = "${featureFlagKey.key} feature flag is not enabled",
                        ),
                    ),
                )

            notification in notificationRegistry -> {
                val id = checkNotNull(notificationRegistry[notification]) {
                    "Unexcepted state when trying to dismiss a notification. " +
                        "The required notification was not found in registry." +
                        "This might have been caused by a concurrent modification of the registry." +
                        "Please report this issue." +
                        "Notification = $notification" +
                        "Registrar = ${notificationRegistry.registrar}"
                }
                notifier.dismiss(id)
                Outcome.success(Success(notificationId = id, command = this))
            }

            else -> {
                Outcome.failure(
                    Failure(
                        command = this,
                        throwable = NotificationCommandException("Can't execute command."),
                    ),
                )
            }
        }
    }
}
