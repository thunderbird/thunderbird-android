package net.thunderbird.feature.notification.sender

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.command.NotificationCommand
import net.thunderbird.feature.notification.command.NotificationCommand.CommandOutcome.Failure
import net.thunderbird.feature.notification.command.NotificationCommand.CommandOutcome.Success
import net.thunderbird.feature.notification.command.NotificationCommandFactory
import net.thunderbird.feature.notification.content.Notification

/**
 * Responsible for sending notifications by creating and executing the appropriate commands.
 *
 * This class utilizes a [NotificationCommandFactory] to generate a list of
 * [NotificationCommand]s based on the provided [Notification]. It then executes
 * each command and emits the result of the execution as a [Flow].
 *
 * @param commandFactory The factory used to create notification commands.
 */
class NotificationSender internal constructor(
    private val commandFactory: NotificationCommandFactory,
) {
    /**
     * Sends a notification by creating and executing the appropriate commands.
     *
     * This function takes a [Notification] object, uses the [commandFactory] to generate
     * a list of [NotificationCommand]s tailored to that notification, and then executes
     * each command sequentially. The result of each command execution ([NotificationCommand.CommandOutcome])
     * is emitted as part of the returned [Flow].
     *
     * @param notification The [Notification] to be sent.
     * @return A [Flow] that emits the [NotificationCommand.CommandOutcome] for each executed command.
     */
    fun send(notification: Notification): Flow<Outcome<Success<Notification>, Failure<Notification>>> = flow {
        val commands = commandFactory.create(notification)
        commands.forEach { command ->
            emit(command.execute())
        }
    }
}
