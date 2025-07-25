package net.thunderbird.feature.notification.impl

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.content.Notification

@OptIn(ExperimentalAtomicApi::class)
class DefaultNotificationRegistry : NotificationRegistry {
    private val mutex = Mutex()

    // We use a MutableMap<Notification, NotificationId>, rather than MutableMap<NotificationId, Notification>,
    // allowing for quick lookups (O(1) on average for MutableMap) to check if a notification is already present
    // during registration.
    private val _registrar = mutableMapOf<Notification, NotificationId>()
    private val rawId = AtomicInt(value = 0)

    override val registrar: Map<NotificationId, Notification> get() = _registrar
        .entries
        .associate { (notification, notificationId) -> notificationId to notification }

    override fun get(notificationId: NotificationId): Notification? {
        return _registrar
            .entries
            .firstOrNull { (_, value) -> value == notificationId }
            ?.key
    }

    override fun get(notification: Notification): NotificationId? {
        return _registrar[notification]
    }

    override suspend fun register(notification: Notification): NotificationId {
        return mutex.withLock {
            val existingNotificationId = get(notification)
            if (existingNotificationId != null) {
                return@withLock existingNotificationId
            }

            val id = rawId.incrementAndFetch()
            val notificationId = NotificationId(id)
            _registrar.put(notification, notificationId)

            notificationId
        }
    }

    override fun unregister(notificationId: NotificationId) {
        val notification = get(notificationId)
        _registrar.remove(notification)
    }

    override fun unregister(notification: Notification) {
        _registrar.remove(notification)
    }
}
