package net.thunderbird.feature.notification.impl

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
) : NotificationRegistry {
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutex = Mutex()

    private val internalRegistrar = MutableStateFlow<Registrar>(Registrar())
    private val rawId = AtomicInt(value = 0)

    override val registrar: StateFlow<Map<NotificationId, Notification>> = internalRegistrar
        .map { current -> current.byId }
        .stateIn(scope, started = SharingStarted.WhileSubscribed(), initialValue = emptyMap())

    override fun get(notificationId: NotificationId): Notification? = internalRegistrar.value[notificationId]

    override fun get(notification: Notification): NotificationId? = internalRegistrar.value[notification]

    override suspend fun register(notification: Notification): NotificationId {
        return mutex.withLock {
            val existingNotificationId = get(notification)
            if (existingNotificationId != null) {
                return@withLock existingNotificationId
            }

            val id = rawId.incrementAndFetch()
            val notificationId = NotificationId(id)
            internalRegistrar.update { it + (notification to notificationId) }

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
        internalRegistrar.update { it - notification }
    }

    override fun contains(notification: Notification): Boolean = notification in internalRegistrar.value

    override fun contains(notificationId: NotificationId): Boolean = notificationId in internalRegistrar.value

    private data class Registrar(
        val byNotification: Map<Notification, NotificationId> = emptyMap(),
        val byId: Map<NotificationId, Notification> = emptyMap(),
    ) {
        operator fun get(id: NotificationId): Notification? = byId[id]
        operator fun get(notification: Notification): NotificationId? = byNotification[notification]
        operator fun contains(id: NotificationId): Boolean = id in byId
        operator fun contains(notification: Notification): Boolean = notification in byNotification

        operator fun plus(pair: Pair<Notification, NotificationId>): Registrar {
            return copy(
                byNotification = byNotification + pair,
                byId = byId + (pair.second to pair.first),
            )
        }

        operator fun minus(notification: Notification): Registrar {
            val id = byNotification[notification] ?: return this
            return copy(
                byNotification = byNotification - notification,
                byId = byId - id,
            )
        }
    }
}
