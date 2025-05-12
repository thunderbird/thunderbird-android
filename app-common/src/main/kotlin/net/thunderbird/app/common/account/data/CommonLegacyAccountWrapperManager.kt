package net.thunderbird.app.common.account.data

import app.k9mail.legacy.account.AccountManager
import app.k9mail.legacy.account.LegacyAccountWrapper
import app.k9mail.legacy.account.LegacyAccountWrapperManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
