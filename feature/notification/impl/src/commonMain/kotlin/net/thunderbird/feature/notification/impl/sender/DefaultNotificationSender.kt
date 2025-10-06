package net.thunderbird.feature.notification.impl.sender

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.command.NotificationCommand.Failure
import net.thunderbird.feature.notification.api.command.NotificationCommand.Success
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.sender.NotificationSender
import net.thunderbird.feature.notification.impl.command.NotificationCommandFactory

/**
 * Responsible for sending notifications by creating and executing the appropriate commands.
 *
 * This class utilizes a [NotificationCommandFactory] to generate a list of
 * [NotificationCommand]s based on the provided [Notification]. It then executes
 * each command and emits the result of the execution as a [Flow].
 *
 * @param commandFactory The factory used to create notification commands.
 */
class DefaultNotificationSender internal constructor(
    private val commandFactory: NotificationCommandFactory,
) : NotificationSender {
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
    override fun send(notification: Notification): Flow<Outcome<Success<Notification>, Failure<Notification>>> = flow {
        val commands = commandFactory.create(notification)
        commands.forEach { command ->
            emit(command.execute())
        }
    }
}
