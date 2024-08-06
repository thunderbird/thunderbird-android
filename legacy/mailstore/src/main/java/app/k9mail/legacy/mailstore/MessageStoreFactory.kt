package app.k9mail.legacy.mailstore

import app.k9mail.legacy.account.Account

interface MessageStoreFactory {
    fun create(account: Account): ListenableMessageStore
}
