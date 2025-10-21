package net.thunderbird.feature.notification.impl

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.content.Notification

@OptIn(ExperimentalAtomicApi::class)
class DefaultNotificationRegistry(
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val scope: CoroutineScope = CoroutineScope(dispatcher),
) : NotificationRegistry {
    private val mutex = Mutex()

    // We use a MutableMap<Notification, NotificationId>, rather than MutableMap<NotificationId, Notification>,
    // allowing for quick lookups (O(1) on average for MutableMap) to check if a notification is already present
    // during registration.
    private val _registrar = MutableStateFlow<Map<Notification, NotificationId>>(emptyMap())
    private val rawId = AtomicInt(value = 0)

    override val registrar: StateFlow<Map<NotificationId, Notification>>
        get() = _registrar
            .map { current -> current.map { it.value to it.key }.toMap() }
            .stateIn(scope, started = SharingStarted.Eagerly, initialValue = emptyMap())

    override fun get(notificationId: NotificationId): Notification? {
        return _registrar
            .value
            .entries
            .firstOrNull { (_, value) -> value == notificationId }
            ?.key
    }

    override fun get(notification: Notification): NotificationId? {
        return _registrar.value[notification]
    }

    override suspend fun register(notification: Notification): NotificationId {
        return mutex.withLock {
            val existingNotificationId = get(notification)
            if (existingNotificationId != null) {
                return@withLock existingNotificationId
            }

            val id = rawId.incrementAndFetch()
            val notificationId = NotificationId(id)
            _registrar.update { it + (notification to notificationId) }

            notificationId
        }
    }

    override fun unregister(notificationId: NotificationId) {
        val notification = get(notificationId)
        if (notification != null) {
            unregister(notification)
        }
    }

    override fun unregister(notification: Notification) {
        _registrar.update { it - notification }
    }

    override fun contains(notification: Notification): Boolean {
        return _registrar.value.containsKey(notification)
    }

    override fun contains(notificationId: NotificationId): Boolean {
        return _registrar.value.containsValue(notificationId)
    }
}
