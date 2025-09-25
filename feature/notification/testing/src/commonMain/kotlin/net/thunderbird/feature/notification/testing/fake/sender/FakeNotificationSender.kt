package net.thunderbird.feature.notification.testing.fake.sender

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import net.thunderbird.feature.notification.api.command.outcome.NotificationCommandOutcome
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.sender.NotificationSender

class FakeNotificationSender(
    private val results: List<NotificationCommandOutcome<Notification>>,
) : NotificationSender {
    override fun send(notification: Notification): Flow<NotificationCommandOutcome<Notification>> =
        results.asFlow()
}
