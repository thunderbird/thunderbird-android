package net.thunderbird.feature.notification.api.receiver

import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.content.Notification

/**
 * Interface for displaying notifications.
 *
 * This is a sealed interface, meaning that all implementations must be declared in this file.
 *
 * @param TNotification The type of notification to display.
 */
interface NotificationNotifier<in TNotification : Notification> {
    /**
     * Shows a notification to the user.
     *
     * @param id The notification id. Mostly used by System Notifications.
     * @param notification The notification to show.
     */
    fun show(id: NotificationId, notification: TNotification)

    /**
     * Disposes of any resources used by the notifier.
     *
     * This should be called when the notifier is no longer needed to prevent memory leaks.
     */
    fun dispose()

    object TypeQualifier {
        object InAppNotification
        object SystemNotification
    }
}
