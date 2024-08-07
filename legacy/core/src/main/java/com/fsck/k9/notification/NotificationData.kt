package com.fsck.k9.notification

import app.k9mail.legacy.account.Account
import app.k9mail.legacy.message.controller.MessageReference

/**
 * Holds information about active and inactive new message notifications of an account.
 */
internal data class NotificationData(
    val account: Account,
    val activeNotifications: List<NotificationHolder>,
    val inactiveNotifications: List<InactiveNotificationHolder>,
) {
    val newMessagesCount: Int
        get() = activeNotifications.size + inactiveNotifications.size

    val isSingleMessageNotification: Boolean
        get() = activeNotifications.size == 1

    val messageReferences: List<MessageReference>
        get() {
            return buildList(capacity = newMessagesCount) {
                for (activeNotification in activeNotifications) {
                    add(activeNotification.content.messageReference)
                }
                for (inactiveNotification in inactiveNotifications) {
                    add(inactiveNotification.content.messageReference)
                }
            }
        }

    val activeMessageReferences: List<MessageReference>
        get() = activeNotifications.map { it.content.messageReference }

    fun isEmpty() = activeNotifications.isEmpty()

    companion object {
        fun create(account: Account): NotificationData {
            return NotificationData(account, activeNotifications = emptyList(), inactiveNotifications = emptyList())
        }
    }
}
