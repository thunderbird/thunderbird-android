package net.thunderbird.feature.notification.testing.fake.receiver

import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier

class FakeSystemNotificationNotifier : NotificationNotifier<SystemNotification> {
    override suspend fun show(
        id: NotificationId,
        notification: SystemNotification,
    ) = Unit

    override fun dispose() = Unit
}
