package app.k9mail.legacy.mailstore

import net.thunderbird.feature.account.AccountId

internal class FakeMessageStoreFactory(
    private val messageStoresById: Map<AccountId, ListenableMessageStore>,
) : MessageStoreFactory {
    override fun create(accountId: AccountId): ListenableMessageStore =
        messageStoresById[accountId] ?: error("Account not found: $accountId")
}
