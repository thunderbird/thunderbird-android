package net.thunderbird.app.common.feature.account

import net.thunderbird.app.common.account.data.DefaultLegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountRepository

internal class DefaultAccountRepository(
    private val accountManager: DefaultLegacyAccountManager,
) : AccountRepository {
    override fun delete(accountId: AccountId) {
        accountManager.delete(accountId)
    }
}
