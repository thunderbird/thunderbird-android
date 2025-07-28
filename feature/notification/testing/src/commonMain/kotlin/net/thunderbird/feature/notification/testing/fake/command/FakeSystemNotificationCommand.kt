package net.thunderbird.feature.notification.testing.fake.command

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.testing.fake.FakeSystemOnlyNotification
import net.thunderbird.feature.notification.testing.fake.receiver.FakeSystemNotificationNotifier

class FakeSystemNotificationCommand(
    notification: SystemNotification = FakeSystemOnlyNotification(),
    notifier: NotificationNotifier<SystemNotification> = FakeSystemNotificationNotifier(),
) : NotificationCommand<SystemNotification>(notification, notifier) {
    override suspend fun execute(): Outcome<Success<SystemNotification>, Failure<SystemNotification>> =
        error("not implemented")
}
