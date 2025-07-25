package net.thunderbird.feature.notification.testing.fake.receiver

import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier

open class FakeInAppNotificationNotifier : NotificationNotifier<InAppNotification> {
    override suspend fun show(
        id: NotificationId,
        notification: InAppNotification,
    ) = Unit

    override fun dispose() = Unit
}
