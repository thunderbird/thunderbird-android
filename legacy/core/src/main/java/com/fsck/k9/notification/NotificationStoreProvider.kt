package com.fsck.k9.notification

import app.k9mail.legacy.account.LegacyAccount

interface NotificationStoreProvider {
    fun getNotificationStore(account: LegacyAccount): NotificationStore
}
