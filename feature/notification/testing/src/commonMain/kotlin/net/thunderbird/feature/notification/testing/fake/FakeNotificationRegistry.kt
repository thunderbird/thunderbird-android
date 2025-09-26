package net.thunderbird.feature.notification.testing.fake

import kotlin.random.Random
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.content.Notification

open class FakeNotificationRegistry : NotificationRegistry {
    private val byId = mutableMapOf<NotificationId, Notification>()
    private val byNotification = mutableMapOf<Notification, NotificationId>()

    override val registrar: Map<NotificationId, Notification>
        get() = byId

    override fun get(notificationId: NotificationId): Notification? = byId[notificationId]

    fun getValue(notificationId: NotificationId): Notification = byId.getValue(notificationId)

    override fun get(notification: Notification): NotificationId? = byNotification[notification]

    fun getValue(notification: Notification): NotificationId = byNotification.getValue(notification)

    override suspend fun register(notification: Notification): NotificationId {
        val id = NotificationId(value = Random.nextInt())
        byId[id] = notification
        byNotification[notification] = id
        return id
    }

    override fun unregister(notificationId: NotificationId) {
        byId.remove(notificationId)?.let { notif ->
            byNotification.remove(notif)
        }
    }

    override fun unregister(notification: Notification) {
        byNotification.remove(notification)?.let { id ->
            byId.remove(id)
        }
    }

    override fun contains(notification: Notification): Boolean = byNotification.containsKey(notification)

    override fun contains(notificationId: NotificationId): Boolean = byId.containsKey(notificationId)
}
