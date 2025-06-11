package net.thunderbird.feature.notification.api

import net.thunderbird.core.common.io.KmpParcelable
import net.thunderbird.core.common.io.KmpParcelize

/**
 * Defines the appearance of notifications on the lock screen.
 */
@KmpParcelize
sealed interface LockscreenNotificationAppearance : KmpParcelable {
    /**
     * No notifications are shown on the lock screen.
     */
    data object None : LockscreenNotificationAppearance

    /**
     * Only the app name is shown on the lock screen for new messages.
     */
    data object AppName : LockscreenNotificationAppearance

    /**
     * All the notification content's is shown on the lock screen.
     */
    data object Public : LockscreenNotificationAppearance

    /**
     * The number of new messages is shown on the lock screen.
     */
    data object MessageCount : LockscreenNotificationAppearance

    /**
     * The names of the senders of new messages are shown on the lock screen.
     */
    data class SenderNames(val senderNames: String) : LockscreenNotificationAppearance
}
