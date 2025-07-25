package net.thunderbird.feature.notification.impl.receiver

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.thunderbird.feature.notification.api.receiver.InAppNotificationEvent
import net.thunderbird.feature.notification.api.receiver.InAppNotificationReceiver

/**
 * An event bus for in-app notifications.
 *
 * This interface extends [InAppNotificationReceiver] to allow listening for notification events,
 * and adds a [publish] method to send new notification events.
 */
internal interface InAppNotificationEventBus : InAppNotificationReceiver {
    /**
     * Publishes an in-app notification event to the event bus.
     *
     * @param event The [InAppNotificationEvent] to be published.
     */
    suspend fun publish(event: InAppNotificationEvent)
}

internal fun InAppNotificationEventBus(): InAppNotificationEventBus = object : InAppNotificationEventBus {
    private val _events = MutableSharedFlow<InAppNotificationEvent>(replay = 1)
    override val events: SharedFlow<InAppNotificationEvent> = _events.asSharedFlow()

    override suspend fun publish(event: InAppNotificationEvent) {
        _events.emit(event)
    }
}
