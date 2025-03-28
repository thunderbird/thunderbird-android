package app.k9mail.legacy.mailstore

import app.k9mail.legacy.account.LegacyAccount

interface MessageStoreFactory {
    fun create(account: LegacyAccount): ListenableMessageStore
}
