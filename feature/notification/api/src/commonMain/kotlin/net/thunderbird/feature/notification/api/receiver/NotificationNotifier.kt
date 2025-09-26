package net.thunderbird.feature.notification.api.receiver

import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.content.Notification

/**
 * Abstraction for components that present and manage a specific kind of notification.
 *
 * Implementations are responsible for rendering notifications (e.g., system tray notifications,
 * in-app notifications) and for dismissing them when requested. The generic type parameter
 * allows an implementation to declare which [Notification] sub-type it can handle.
 *
 * Type parameters:
 * @param TNotification The specific subtype of [Notification] this notifier can display. The
 * contravariant `in` variance allows a notifier for a base type to also accept its subtypes.
 */
interface NotificationNotifier<in TNotification : Notification> {
    /**
     * Displays or updates a notification associated with the given [id].
     *
     * Implementations should render [notification] according to their medium. If a notification
     * with the same [id] is already visible, this call should update/replace it when supported
     * by the underlying mechanism.
     *
     * @param id A stable identifier that correlates to this notification instance across updates
     * and dismissal. Often maps to a system notification ID when using platform notifications.
     * @param notification The domain model describing what to present to the user.
     */
    suspend fun show(id: NotificationId, notification: TNotification)

    /**
     * Dismisses the notification previously shown with [id].
     *
     * If no notification is currently displayed for [id], implementations should treat this as a
     * no-op.
     *
     * @param id The identifier of the notification to dismiss.
     */
    suspend fun dismiss(id: NotificationId)

    /**
     * Disposes of any resources used by the notifier.
     *
     * This should be called when the notifier is no longer needed to prevent memory leaks.
     */
    fun dispose()
}
