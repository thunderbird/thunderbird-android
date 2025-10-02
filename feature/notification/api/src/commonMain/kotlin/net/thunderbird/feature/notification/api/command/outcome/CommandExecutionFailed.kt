package net.thunderbird.feature.notification.api.command.outcome

import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.content.Notification

/**
 * Represents a failure that occurred while attempting to execute a command.
 *
 * Use this when a command was recognized and supported, but its execution failed due to an error
 * (e.g., I/O failure, unexpected state, or an exception thrown by the underlying system).
 *
 * @param TNotification The type of notification associated with the command.
 * @property command The command whose execution failed. May be null if the command couldn't be
 *                   fully constructed.
 * @property message A human-readable description of the failure. Defaults to a generic message.
 * @property throwable An optional exception that provides additional context about the failure.
 */
data class CommandExecutionFailed<out TNotification : Notification>(
    override val command: NotificationCommand<out TNotification>,
    val message: String = "Command failed to execute.",
    val throwable: Throwable? = null,
) : Failure<TNotification>
