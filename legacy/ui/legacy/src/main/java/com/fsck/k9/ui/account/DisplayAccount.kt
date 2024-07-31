package com.fsck.k9.ui.account

import app.k9mail.legacy.account.Account

data class DisplayAccount(
    val account: Account,
    val unreadMessageCount: Int,
    val starredMessageCount: Int,
)
