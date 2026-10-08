package net.thunderbird.feature.notification.testing.fake.receiver

import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier
import net.thunderbird.feature.notification.testing.fake.FakeNotificationRegistry

abstract class BaseFakeNotificationNotifier<T : Notification> internal constructor(
    private val registry: NotificationRegistry = FakeNotificationRegistry(),
) : NotificationNotifier<T> {
    val shownNotifications = mutableListOf<T>()
    val dismissedNotificationIds = mutableListOf<NotificationId>()

    override suspend fun show(
        notification: T,
    ): NotificationId {
        shownNotifications += notification
        return registry.register(notification)
    }

    override suspend fun dismiss(id: NotificationId) {
        dismissedNotificationIds += id
        registry.unregister(id)
    }

    override fun dispose() = Unit
}
