package net.thunderbird.feature.notification.impl.command

import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.logging.LogTag
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.command.outcome.CommandExecutionFailed
import net.thunderbird.feature.notification.api.command.outcome.NotificationCommandOutcome
import net.thunderbird.feature.notification.api.command.outcome.Success
import net.thunderbird.feature.notification.api.command.outcome.UnsupportedCommand
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.impl.DefaultNotificationRegistry

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

    override suspend fun execute(): NotificationCommandOutcome<TNotification> {
        logger.verbose(logTag) { "execute() called with: notification = $notification" }
        return when {
            featureFlagProvider.provide(featureFlagKey).isDisabledOrUnavailable() ->
                Outcome.failure(
                    error = UnsupportedCommand(
                        command = this,
                        reason = UnsupportedCommand.Reason.FeatureFlagDisabled(key = featureFlagKey),
                    ),
                )

            notification in notificationRegistry -> {
                val id = checkNotNull(notificationRegistry[notification]) {
                    "Unexcepted state when trying to dismiss a notification. " +
                        "The required notification was not found in registry." +
                        "This might have been caused by a concurrent modification of the registry." +
                        "Please report this issue." +
                        "Notification = $notification" +
                        (notificationRegistry as? DefaultNotificationRegistry)?.registrar?.let { ", Registrar = $it" }
                }
                notifier.dismiss(id)
                Outcome.success(Success(notificationId = id, command = this))
            }

            else -> Outcome.failure(
                error = CommandExecutionFailed(
                    command = this,
                    message = "Notification is not registered in the NotificationRegistry.",
                ),
            )
        }
    }
}
