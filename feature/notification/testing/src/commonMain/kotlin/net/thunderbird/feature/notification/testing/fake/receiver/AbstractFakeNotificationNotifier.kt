package net.thunderbird.feature.notification.testing.fake.receiver

import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.testing.fake.FakeNotificationRegistry

abstract class AbstractFakeNotificationNotifier<T : Notification> internal constructor(
    private val registry: NotificationRegistry = FakeNotificationRegistry(),
) : NotificationNotifier<T> {
    override suspend fun show(
        notification: T,
    ): NotificationId = registry.register(notification)

    override suspend fun dismiss(id: NotificationId) = registry.unregister(id)

    override fun dispose() = Unit
}
