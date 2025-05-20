package net.thunderbird.app.common.account.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.android.account.LegacyAccountWrapper
import net.thunderbird.core.android.account.LegacyAccountWrapperManager
import net.thunderbird.feature.account.storage.legacy.mapper.LegacyAccountWrapperDataMapper

internal class DefaultLegacyAccountWrapperManager(
    private val accountManager: AccountManager,
    private val accountDataMapper: LegacyAccountWrapperDataMapper,
) : LegacyAccountWrapperManager {

    override fun getAll(): Flow<List<LegacyAccountWrapper>> {
        return accountManager.getAccountsFlow()
            .map { list ->
                list.map { account ->
                    accountDataMapper.toDomain(account)
                }
            }
    }

    override fun getById(id: String): Flow<LegacyAccountWrapper?> {
        return accountManager.getAccountFlow(id).map { account ->
            account?.let {
                accountDataMapper.toDomain(it)
            }
        }
    }

    override suspend fun update(account: LegacyAccountWrapper) {
        accountManager.saveAccount(
            accountDataMapper.toDto(account),
        )
    }
}
