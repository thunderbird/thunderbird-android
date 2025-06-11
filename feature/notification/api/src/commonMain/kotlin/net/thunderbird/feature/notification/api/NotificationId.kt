package net.thunderbird.feature.notification.api

import net.thunderbird.core.common.io.KmpParcelable
import net.thunderbird.core.common.io.KmpParcelize

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
@KmpParcelize
value class NotificationId(val value: Int) : KmpParcelable, Comparable<Int> by value {
    companion object {
        val Undefined = NotificationId(value = -1)
    }
}
