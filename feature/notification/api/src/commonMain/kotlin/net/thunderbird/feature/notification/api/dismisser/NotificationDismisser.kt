package net.thunderbird.feature.notification.api.dismisser

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.command.NotificationCommand.Failure
import net.thunderbird.feature.notification.api.command.NotificationCommand.Success
import net.thunderbird.feature.notification.api.content.Notification

/**
 * Responsible for dismissing notifications by creating and executing the appropriate commands.
 */
interface NotificationDismisser {
    /**
     * Dismisses a notification with the given ID.
     *
     * @param id The ID of the notification to dismiss.
     * @return A [Flow] of [Outcome] that emits either a [Success] with the dismissed [Notification]
     * or a [Failure] with the [Notification] that failed to be dismissed.
     */
    fun dismiss(id: NotificationId): Flow<Outcome<Success<Notification>, Failure<Notification>>>

    /**
     * Dismisses a notification.
     *
     * @param notification The notification to dismiss.
     * @return A [Flow] of [Outcome] that emits the result of the dismiss operation.
     * The [Outcome] will be a [Success] containing the dismissed [Notification] if the operation was successful,
     * or a [Failure] containing the [Notification] if the operation failed.
     */
    fun dismiss(notification: Notification): Flow<Outcome<Success<Notification>, Failure<Notification>>>
}
