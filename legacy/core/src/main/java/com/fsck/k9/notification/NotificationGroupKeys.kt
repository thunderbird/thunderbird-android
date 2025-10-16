package com.fsck.k9.notification

import net.thunderbird.core.android.account.LegacyAccountDto

object NotificationGroupKeys {
    private const val NOTIFICATION_GROUP_KEY_PREFIX = "newMailNotifications-"

    fun getGroupKey(account: LegacyAccountDto): String {
        return NOTIFICATION_GROUP_KEY_PREFIX + account.accountNumber
    }
}
