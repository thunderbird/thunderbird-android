package net.thunderbird.feature.notification.api

import net.thunderbird.feature.notification.api.content.Notification

/**
 * A registry for managing notifications and their corresponding IDs.
 *
 * It establishes and maintains the correlation between a [Notification] object
 * and its unique [NotificationId].
 * This can also be used to track which notifications are currently being displayed
 * to the user.
 */
interface NotificationRegistry {
    /**
     * A [Map] off all the current notifications, associated with their IDs,
     * being displayed to the user.
     */
    val registrar: Map<NotificationId, Notification>

    /**
     * Retrieves a [Notification] object based on its [notificationId].
     *
     * @param notificationId The ID of the notification to retrieve.
     * @return The [Notification] object associated with the given [notificationId],
     * or `null` if no such notification exists.
     */
    operator fun get(notificationId: NotificationId): Notification?

    /**
     * Retrieves the [NotificationId] associated with the given [notification].
     *
     * @param notification The notification for which to retrieve the ID.
     * @return The [NotificationId] if the notification is registered, or `null` otherwise.
     */
    operator fun get(notification: Notification): NotificationId?

    /**
     * Registers a notification and returns its unique ID.
     *
     * If the provided [notification] is already registered, this function will effectively
     * return its known [NotificationId].
     *
     * @param notification The [Notification] object to register.
     * @return The unique [NotificationId] assigned to the registered notification.
     */
    suspend fun register(notification: Notification): NotificationId

    /**
     * Unregisters a [Notification] by its [NotificationId].
     *
     * @param notificationId The ID of the notification to unregister.
     */
    fun unregister(notificationId: NotificationId)

    /**
     * Unregisters a previously registered notification.
     *
     * @param notification The [Notification] object to unregister.
     */
    fun unregister(notification: Notification)

    /**
     * Checks if a specific notification is currently registered.
     *
     * @param notification The [Notification] object to check.
     * @return `true` if the notification is registered, `false` otherwise.
     */
    operator fun contains(notification: Notification): Boolean

    /**
     * Checks if a notification with the given [notificationId] is currently registered.
     *
     * @param notificationId The ID of the notification to check.
     * @return `true` if the notification is registered, `false` otherwise.
     */
    operator fun contains(notificationId: NotificationId): Boolean
}
