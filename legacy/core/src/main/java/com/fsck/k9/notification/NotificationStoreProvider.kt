package com.fsck.k9.notification

import net.thunderbird.core.android.account.LegacyAccount

interface NotificationStoreProvider {
    fun getNotificationStore(account: LegacyAccount): NotificationStore
}
