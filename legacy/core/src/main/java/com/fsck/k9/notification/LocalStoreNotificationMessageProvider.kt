package com.fsck.k9.notification

import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.mailstore.NotificationMessage
import net.thunderbird.feature.account.AccountId

internal class LocalStoreNotificationMessageProvider(
    private val localStoreProvider: LocalStoreProvider,
) : NotificationMessageProvider {
    override fun getNotificationMessages(accountId: AccountId): List<NotificationMessage> {
        return localStoreProvider.getInstance(accountId).notificationMessages
    }
}
