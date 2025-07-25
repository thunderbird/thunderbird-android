package net.thunderbird.feature.notification.impl.ui.action

import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.ui.action.NotificationAction

/**
 * Interface responsible for creating Android-specific notification actions ([AndroidNotificationAction])
 * from generic notification actions ([NotificationAction]).
 *
 * This allows decoupling the core notification logic from the Android platform specifics.
 *
 * @param TNotification The type of [Notification] this creator can handle.
 */
interface NotificationActionCreator<TNotification : Notification> {
    /**
     * Creates an [AndroidNotificationAction] for the given [notification] and [action].
     *
     * @param notification The notification to create the action for.
     * @param action The action to create.
     * @return The created [AndroidNotificationAction].
     */
    suspend fun create(notification: TNotification, action: NotificationAction): AndroidNotificationAction

    enum class TypeQualifier { System, InApp }
}
