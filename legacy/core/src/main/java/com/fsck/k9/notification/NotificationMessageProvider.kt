package com.fsck.k9.notification

import com.fsck.k9.mailstore.NotificationMessage
import net.thunderbird.feature.account.AccountId

/**
 * Provides the persisted new message notifications of an account.
 */
internal fun interface NotificationMessageProvider {
    fun getNotificationMessages(accountId: AccountId): List<NotificationMessage>
}
