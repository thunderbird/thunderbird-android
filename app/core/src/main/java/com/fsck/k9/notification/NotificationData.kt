package com.fsck.k9.notification

import com.fsck.k9.Account

/**
 * Holds information about active and inactive new message notifications of an account.
 */
internal data class NotificationData(
    val account: Account,
    val activeNotifications: List<NotificationHolder>,
    val inactiveNotifications: List<InactiveNotificationHolder>
) {
    val newMessagesCount: Int
        get() = activeNotifications.size + inactiveNotifications.size

    val isSingleMessageNotification: Boolean
        get() = activeNotifications.size == 1

    fun isEmpty() = activeNotifications.isEmpty()

    companion object {
        fun create(account: Account): NotificationData {
            return NotificationData(account, activeNotifications = emptyList(), inactiveNotifications = emptyList())
        }
    }
}
