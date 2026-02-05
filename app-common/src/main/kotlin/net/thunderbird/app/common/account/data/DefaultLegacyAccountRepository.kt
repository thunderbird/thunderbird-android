package net.thunderbird.app.common.account.data

import kotlinx.coroutines.flow.flow
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.android.account.LegacyAccountRepository
import net.thunderbird.feature.account.AccountId

class DefaultLegacyAccountRepository(private val accountManager: LegacyAccountManager) : LegacyAccountRepository {
    override suspend fun getById(id: AccountId) = flow {
        emit(accountManager.getAccount(id.toString()))
    }

    override suspend fun update(account: LegacyAccount) {
        accountManager.update(account)
    }
}
