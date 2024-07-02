package com.fsck.k9.notification

import com.fsck.k9.Account

interface NotificationStoreProvider {
    fun getNotificationStore(account: Account): NotificationStore
}
