package net.thunderbird.app.common.account.data

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.android.account.LegacyAccountRepository
import net.thunderbird.feature.account.AccountId

class DefaultLegacyAccountRepository(private val accountManager: LegacyAccountManager) : LegacyAccountRepository {
    override fun observeById(accountId: AccountId) = accountManager.getByIdFlow(accountId)

    override suspend fun update(account: LegacyAccount) {
        accountManager.update(account)
    }
}
