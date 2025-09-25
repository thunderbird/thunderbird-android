package net.thunderbird.feature.notification.api.command

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier

/**
 * Represents a command that can be executed on a notification.
 *
 * This class is the base for all notification commands. It defines the basic structure
 * of a command and the possible outcomes of its execution.
 *
 * @param TNotification The type of notification this command operates on.
 * @property notification The notification instance this command will act upon.
 * @property notifier The notifier responsible for handling the notification.
 */
abstract class NotificationCommand<TNotification : Notification>(
    protected val notification: TNotification,
    protected val notifier: NotificationNotifier<TNotification>,
) {
    /**
     * Executes the command.
     * @return The result of the execution.
     */
    abstract suspend fun execute(): Outcome<Success<TNotification>, Failure<TNotification>>

    /**
     * Represents the outcome of a command's execution.
     */
    sealed interface CommandOutcome

    /**
     * Represents a successful command execution.
     *
     * @param TNotification The type of notification associated with the command.
     * @property command The command that was executed successfully.
     */
    data class Success<out TNotification : Notification>(
        val command: NotificationCommand<out TNotification>,
    ) : CommandOutcome

    /**
     * Represents a failed command execution.
     *
     * @param TNotification The type of notification associated with the command.
     * @property command The command that failed.
     * @property throwable The exception that caused the failure.
     */
    data class Failure<out TNotification : Notification>(
        val command: NotificationCommand<out TNotification>?,
        val throwable: Throwable,
    ) : CommandOutcome
}
