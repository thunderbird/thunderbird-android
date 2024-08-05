package com.fsck.k9.mailstore

import app.k9mail.legacy.account.Account

interface MessageStoreFactory {
    fun create(account: Account): ListenableMessageStore
}
