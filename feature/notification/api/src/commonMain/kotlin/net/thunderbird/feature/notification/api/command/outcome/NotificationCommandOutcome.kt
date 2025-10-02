@file:JvmName("NotificationCommandOutcome")

package net.thunderbird.feature.notification.api.command.outcome

import androidx.annotation.Discouraged
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.content.Notification

/**
 * Outcome type for executing a NotificationCommand.
 *
 * When executing a command, the result will be either a [Success] or a [Failure].
 */
typealias NotificationCommandOutcome<TNotification> = Outcome<Success<TNotification>, Failure<TNotification>>

/**
 * Represents a successful command execution.
 *
 * @param TNotification The type of notification associated with the command.
 * @property notificationId The ID of the notification that was successfully acted upon.
 * @property command The command that was executed successfully.
 */
sealed interface Success<out TNotification : Notification> {
    val notificationId: NotificationId
    val rawNotificationId: Int
        @Discouraged("This is a utility getter to enable usage in Java code. Use notificationId instead.")
        get() = notificationId.value
    val command: NotificationCommand<out TNotification>?

    /**
     * Indicates that the command was executed as requested.
     *
     * This is the canonical success case that carries the command instance that was executed.
     *
     * @param TNotification The type of notification associated with the command.
     * @property command The concrete command that was executed.
     */
    @ConsistentCopyVisibility
    data class Executed<out TNotification : Notification> internal constructor(
        override val notificationId: NotificationId,
        override val command: NotificationCommand<out TNotification>,
    ) : Success<TNotification>

    /**
     * Indicates that executing the command resulted in no operation.
     *
     * This can be used when the system is already in the desired state or when there is
     * intentionally nothing to do. The [command] may be null if there isn't a specific
     * command instance associated with the no-op result.
     *
     * @param TNotification The type of notification associated with the command.
     * @property command The command related to this no-op outcome, if any.
     */
    data class NoOperation<out TNotification : Notification>(
        override val command: NotificationCommand<out TNotification>? = null,
    ) : Success<TNotification> {
        override val notificationId: NotificationId = NotificationId.Undefined
    }
}

/**
 * Convenience factory that wraps the given command in a [Success.Executed] instance.
 */
fun <TNotification : Notification> Success(
    notificationId: NotificationId,
    command: NotificationCommand<out TNotification>,
): Success<TNotification> = Success.Executed(notificationId, command)

/**
 * Convenience factory that wraps the given command in a [Success.Executed] instance.
 */
@Discouraged(
    message = "This is a utility function to enable usage in Java code. " +
        "Use Success(NotificationId, NotificationCommand) instead.",
)
fun <TNotification : Notification> Success(
    notificationId: Int,
    command: NotificationCommand<out TNotification>,
): Success<TNotification> = Success.Executed(NotificationId(notificationId), command)

/**
 * Represents a failed command execution.
 *
 * @param TNotification The type of notification associated with the command.
 * @property command The command that failed.
 */
sealed interface Failure<out TNotification : Notification> {
    val command: NotificationCommand<out TNotification>?
}
