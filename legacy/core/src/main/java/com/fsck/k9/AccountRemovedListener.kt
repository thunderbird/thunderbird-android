package com.fsck.k9

import app.k9mail.legacy.account.Account

fun interface AccountRemovedListener {
    fun onAccountRemoved(account: Account)
}
