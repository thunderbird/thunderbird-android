package net.thunderbird.feature.notification.testing.fake.sender

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.command.NotificationCommand.Failure
import net.thunderbird.feature.notification.api.command.NotificationCommand.Success
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.sender.NotificationSender

class FakeNotificationSender(
    private val results: List<Outcome<Success<Notification>, Failure<Notification>>>,
) : NotificationSender {
    override fun send(notification: Notification): Flow<Outcome<Success<Notification>, Failure<Notification>>> =
        results.asFlow()
}
