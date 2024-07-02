package com.fsck.k9.ui.account

import com.fsck.k9.Account

data class DisplayAccount(
    val account: Account,
    val unreadMessageCount: Int,
    val starredMessageCount: Int,
)
