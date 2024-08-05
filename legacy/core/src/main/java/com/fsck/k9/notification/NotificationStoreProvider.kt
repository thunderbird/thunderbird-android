package com.fsck.k9.notification

import app.k9mail.legacy.account.Account

interface NotificationStoreProvider {
    fun getNotificationStore(account: Account): NotificationStore
}
