package net.thunderbird.feature.notification.impl.intent.action

import android.app.PendingIntent
import net.thunderbird.feature.notification.api.ui.action.NotificationAction

/**
 * Interface for creating a [PendingIntent] for a given [NotificationAction].
 *
 * This interface is used to decouple the creation of [PendingIntent]s from the notification creation logic.
 * Implementations of this interface should be registered in the Koin graph using the [TypeQualifier].
 *
 * @param TNotificationAction The type of [NotificationAction] this creator can handle.
 */
internal interface NotificationActionIntentCreator<in TNotificationAction : NotificationAction> {
    /**
     * Determines whether this [NotificationActionIntentCreator] can create an intent for the given [action].
     *
     * @param action The [NotificationAction] to check.
     * @return `true` if this creator can handle the [action], `false` otherwise.
     */
    fun accept(action: NotificationAction): Boolean

    /**
     * Creates a [PendingIntent] for the given notification action.
     *
     * @param action The notification action to create an intent for.
     * @return The created [PendingIntent], or `null` if the action is not supported or an error occurs.
     */
    fun create(action: TNotificationAction): PendingIntent?

    object TypeQualifier
}
