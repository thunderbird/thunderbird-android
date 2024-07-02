package com.fsck.k9.mailstore

import com.fsck.k9.Account

interface MessageStoreFactory {
    fun create(account: Account): ListenableMessageStore
}
