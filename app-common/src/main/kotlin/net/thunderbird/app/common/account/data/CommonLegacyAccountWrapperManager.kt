package net.thunderbird.app.common.account.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.android.account.LegacyAccountWrapper
import net.thunderbird.core.android.account.LegacyAccountWrapperManager

internal class CommonLegacyAccountWrapperManager(
    private val accountManager: AccountManager,
) : LegacyAccountWrapperManager {

    override fun getAll(): Flow<List<LegacyAccountWrapper>> {
        return accountManager.getAccountsFlow()
            .map { list ->
                list.map { account ->
                    LegacyAccountWrapper.from(account)
                }
            }
    }

    override fun getById(id: String): Flow<LegacyAccountWrapper?> {
        return accountManager.getAccountFlow(id).map { account ->
            account?.let { LegacyAccountWrapper.from(it) }
        }
    }

    override suspend fun update(account: LegacyAccountWrapper) {
        accountManager.saveAccount(LegacyAccountWrapper.to(account))
    }
}
