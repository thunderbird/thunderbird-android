package net.thunderbird.feature.notification.api.receiver

import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.content.SystemNotification

/**
 * A stream of notifications of a specific type.
 *
 * @param TNotification The specific type of [Notification] this stream handles.
 */
interface NotificationStream<TNotification : Notification> {
    val notifications: StateFlow<Set<TNotification>>
}

/**
 * A [NotificationStream] for system notifications.
 *
 * This stream provides a [StateFlow] of the current set of [SystemNotification]s that should be displayed
 * by the operating system's notification service.
 */
interface SystemNotificationStream : NotificationStream<SystemNotification>

/**
 * A specialized [NotificationStream] for handling in-app notifications.
 *
 * This stream provides a [StateFlow] of [InAppNotification]s, which are meant to be
 * displayed within the application's UI, such as in a banner or a snackbar.
 */
interface InAppNotificationStream : NotificationStream<InAppNotification>
