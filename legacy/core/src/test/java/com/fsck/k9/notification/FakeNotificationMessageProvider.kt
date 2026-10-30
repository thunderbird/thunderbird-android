package com.fsck.k9.notification

import com.fsck.k9.mailstore.NotificationMessage
import net.thunderbird.feature.account.AccountId

internal class FakeNotificationMessageProvider : NotificationMessageProvider {
    private val notificationMessages = mutableMapOf<AccountId, MutableList<NotificationMessage>>()

    fun add(accountId: AccountId, notificationMessage: NotificationMessage) {
        notificationMessages.getOrPut(accountId) { mutableListOf() }.add(notificationMessage)
    }

    override fun getNotificationMessages(accountId: AccountId): List<NotificationMessage> {
        return notificationMessages[accountId].orEmpty().toList()
    }
}
