package net.thunderbird.feature.notification.testing.fake

import kotlin.random.Random
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.content.Notification

open class FakeNotificationRegistry : NotificationRegistry {
    override val registrar: Map<NotificationId, Notification>
        get() = TODO("Not yet implemented")

    override fun get(notificationId: NotificationId): Notification? {
        TODO("Not yet implemented")
    }

    override fun get(notification: Notification): NotificationId? {
        TODO("Not yet implemented")
    }

    override suspend fun register(notification: Notification): NotificationId {
        return NotificationId(value = Random.Default.nextInt())
    }

    override fun unregister(notificationId: NotificationId) {
        TODO("Not yet implemented")
    }

    override fun unregister(notification: Notification) {
        TODO("Not yet implemented")
    }
}
