package net.thunderbird.feature.notification.testing.fake

import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.content.Notification

open class FakeNotificationRegistry(
    initialRegistrar: Map<NotificationId, Notification> = mutableMapOf(),
    private val useRandomIdForRegistering: Boolean = true,
) : NotificationRegistry {
    private val internalRegistrar = MutableStateFlow(initialRegistrar)

    override fun get(notificationId: NotificationId): Notification? = internalRegistrar.value[notificationId]

    open fun getValue(notificationId: NotificationId): Notification = internalRegistrar.value.getValue(notificationId)

    override fun get(notification: Notification): NotificationId? = internalRegistrar.value
        .firstNotNullOfOrNull {
            it.takeIf { it.value == notification }?.key
        }

    open fun getValue(notification: Notification): NotificationId = internalRegistrar.value
        .firstNotNullOf {
            it.takeIf { it.value == notification }?.key
        }

    override suspend fun register(notification: Notification): NotificationId {
        val id = NotificationId(
            value = if (useRandomIdForRegistering) Random.nextInt() else internalRegistrar.value.size + 1,
        )
        internalRegistrar.update { it + (id to notification) }
        return id
    }

    override fun unregister(notificationId: NotificationId) {
        internalRegistrar.update { current -> current - notificationId }
    }

    override fun unregister(notification: Notification) {
        internalRegistrar.update { current -> current.filterValues { it != notification } }
    }

    override fun contains(notification: Notification): Boolean = internalRegistrar.value.values.contains(notification)

    override fun contains(notificationId: NotificationId): Boolean = internalRegistrar.value.containsKey(notificationId)
}
