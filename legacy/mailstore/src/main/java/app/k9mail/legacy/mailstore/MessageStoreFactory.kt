package app.k9mail.legacy.mailstore

import net.thunderbird.feature.account.AccountId

interface MessageStoreFactory {
    fun create(accountId: AccountId): ListenableMessageStore
}
