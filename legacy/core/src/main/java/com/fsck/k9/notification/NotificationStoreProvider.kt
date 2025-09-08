package com.fsck.k9.notification

import net.thunderbird.core.android.account.LegacyAccountDto

interface NotificationStoreProvider {
    fun getNotificationStore(account: LegacyAccountDto): NotificationStore
}
