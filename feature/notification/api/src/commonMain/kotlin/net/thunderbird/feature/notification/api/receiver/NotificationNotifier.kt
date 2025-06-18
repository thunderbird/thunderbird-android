package net.thunderbird.feature.notification.api.receiver

import net.thunderbird.feature.notification.api.content.Notification

/**
 * Interface for displaying notifications.
 *
 * This is a sealed interface, meaning that all implementations must be declared in this file.
 *
 * @param TNotification The type of notification to display.
 */
interface NotificationNotifier<in TNotification : Notification> {
    fun show(notification: TNotification)
}
