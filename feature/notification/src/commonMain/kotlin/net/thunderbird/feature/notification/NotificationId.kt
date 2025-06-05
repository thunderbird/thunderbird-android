package net.thunderbird.feature.notification

/**
 * Represents a unique identifier for a notification.
 *
 * This value class wraps an [Int] to provide type safety for notification IDs.
 * It also implements [Comparable] by delegating to the underlying [Int] value,
 * allowing for natural comparison of notification IDs.
 *
 * @property value The integer value of the notification ID.
 */
@JvmInline
value class NotificationId(val value: Int) : Comparable<Int> by value
