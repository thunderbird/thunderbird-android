package net.thunderbird.feature.notification.api.receiver

import kotlinx.coroutines.flow.SharedFlow
import net.thunderbird.feature.notification.api.content.InAppNotification

/**
 * Interface for receiving in-app notification events.
 *
 * This interface provides a [SharedFlow] of [InAppNotificationEvent]s that can be observed
 * by UI components or other parts of the application to react to in-app notifications.
 */
interface InAppNotificationReceiver {
    val events: SharedFlow<InAppNotificationEvent>
}

sealed interface InAppNotificationEvent {
    val notification: InAppNotification

    data class Show(override val notification: InAppNotification) : InAppNotificationEvent
    data class Dismiss(override val notification: InAppNotification) : InAppNotificationEvent
}
