package net.thunderbird.feature.notification.testing.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.NotificationManager
import net.thunderbird.feature.notification.api.command.NotificationCommand.Failure
import net.thunderbird.feature.notification.api.command.NotificationCommand.Success
import net.thunderbird.feature.notification.api.content.Notification

open class FakeNotificationManager(
    private val emitOnSend: (notification: Notification) -> Outcome<Success<Notification>, Failure<Notification>>,
    private val emitOnDismissNotification: (notification: Notification) -> Outcome<
        Success<Notification>,
        Failure<Notification>,
        >,
    private val emitOnDismissId: (id: NotificationId) -> Outcome<Success<Notification>, Failure<Notification>>,
) : NotificationManager {
    override fun send(notification: Notification): Flow<Outcome<Success<Notification>, Failure<Notification>>> = flow {
        emit(emitOnSend(notification))
    }

    override fun dismiss(id: NotificationId): Flow<Outcome<Success<Notification>, Failure<Notification>>> = flow {
        emit(emitOnDismissId(id))
    }

    override fun dismiss(notification: Notification): Flow<Outcome<Success<Notification>, Failure<Notification>>> =
        flow {
            emit(emitOnDismissNotification(notification))
        }
}
