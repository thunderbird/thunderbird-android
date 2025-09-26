package net.thunderbird.feature.notification.testing.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.NotificationManager
import net.thunderbird.feature.notification.api.command.outcome.NotificationCommandOutcome
import net.thunderbird.feature.notification.api.content.Notification

open class FakeNotificationManager(
    private val emitOnSend: (notification: Notification) -> NotificationCommandOutcome<Notification>,
    private val emitOnDismissNotification: (notification: Notification) -> NotificationCommandOutcome<Notification>,
    private val emitOnDismissId: (id: NotificationId) -> NotificationCommandOutcome<Notification>,
) : NotificationManager {
    override fun send(notification: Notification): Flow<NotificationCommandOutcome<Notification>> = flow {
        emit(emitOnSend(notification))
    }

    override fun dismiss(id: NotificationId): Flow<NotificationCommandOutcome<Notification>> = flow {
        emit(emitOnDismissId(id))
    }

    override fun dismiss(notification: Notification): Flow<NotificationCommandOutcome<Notification>> =
        flow {
            emit(emitOnDismissNotification(notification))
        }
}
