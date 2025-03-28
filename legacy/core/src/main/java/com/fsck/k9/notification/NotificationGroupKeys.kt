package com.fsck.k9.notification

import app.k9mail.legacy.account.LegacyAccount

object NotificationGroupKeys {
    private const val NOTIFICATION_GROUP_KEY_PREFIX = "newMailNotifications-"

    fun getGroupKey(account: LegacyAccount): String {
        return NOTIFICATION_GROUP_KEY_PREFIX + account.accountNumber
    }
}
