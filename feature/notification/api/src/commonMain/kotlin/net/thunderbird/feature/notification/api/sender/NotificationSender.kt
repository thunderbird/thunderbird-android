package net.thunderbird.feature.notification.api.sender

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.command.NotificationCommand.CommandOutcome.Failure
import net.thunderbird.feature.notification.api.command.NotificationCommand.CommandOutcome.Success
import net.thunderbird.feature.notification.api.content.Notification

/**
 * Responsible for sending notifications by creating and executing the appropriate commands.
 */
interface NotificationSender {
    /**
     * Sends a notification by creating and executing the appropriate commands.
     *
     * @param notification The [Notification] to be sent.
     * @return A [Flow] that emits the [NotificationCommand.CommandOutcome] for each executed command.
     */
    fun send(notification: Notification): Flow<Outcome<Success<Notification>, Failure<Notification>>>
}
