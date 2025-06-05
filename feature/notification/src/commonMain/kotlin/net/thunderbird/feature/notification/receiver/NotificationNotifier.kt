package net.thunderbird.feature.notification.receiver

import net.thunderbird.feature.notification.content.Notification

/**
 * Interface for displaying notifications.
 *
 * This is a sealed interface, meaning that all implementations must be declared in this file.
 *
 * @param TNotification The type of notification to display.
 */
sealed interface NotificationNotifier<in TNotification : Notification> {
    fun show(notification: TNotification)
}
