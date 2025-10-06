package net.thunderbird.feature.notification.testing.fake.command

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.testing.fake.FakeInAppOnlyNotification
import net.thunderbird.feature.notification.testing.fake.receiver.FakeInAppNotificationNotifier

class FakeInAppNotificationCommand(
    notification: InAppNotification = FakeInAppOnlyNotification(),
    notifier: NotificationNotifier<InAppNotification> = FakeInAppNotificationNotifier(),
) : NotificationCommand<InAppNotification>(notification, notifier) {
    override suspend fun execute(): Outcome<Success<InAppNotification>, Failure<InAppNotification>> =
        error("not implemented")
}
