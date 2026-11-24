package com.fsck.k9.notification

import net.thunderbird.feature.account.AccountId

interface NotificationStoreProvider {
    fun getNotificationStore(accountId: AccountId): NotificationStore
}
