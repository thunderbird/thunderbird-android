package net.thunderbird.app.common.account.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountWrapperManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.storage.legacy.mapper.DefaultLegacyAccountWrapperDataMapper

internal class DefaultLegacyAccountWrapperManager(
    private val accountManager: AccountManager,
    private val accountDataMapper: DefaultLegacyAccountWrapperDataMapper,
) : LegacyAccountWrapperManager {

    override fun getAll(): Flow<List<LegacyAccount>> {
        return accountManager.getAccountsFlow()
            .map { list ->
                list.map { account ->
                    accountDataMapper.toDomain(account)
                }
            }
    }

    override fun getById(id: AccountId): Flow<LegacyAccount?> {
        return accountManager.getAccountFlow(id.asRaw()).map { account ->
            account?.let {
                accountDataMapper.toDomain(it)
            }
        }
    }

    override suspend fun update(account: LegacyAccount) {
        accountManager.saveAccount(
            accountDataMapper.toDto(account),
        )
    }
}
