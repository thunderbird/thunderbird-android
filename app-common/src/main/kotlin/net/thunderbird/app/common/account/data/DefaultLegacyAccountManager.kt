package net.thunderbird.app.common.account.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.storage.legacy.mapper.LegacyAccountDataMapper

@Suppress("TooManyFunctions")
internal class DefaultLegacyAccountManager(
    private val accountManager: LegacyAccountDtoManager,
    private val accountDataMapper: LegacyAccountDataMapper,
) : LegacyAccountManager {

    override fun getAll(): Flow<List<LegacyAccount>> {
        return accountManager.getAccountsFlow()
            .map { list ->
                list.map { account ->
                    accountDataMapper.toDomain(account)
                }
            }
    }

    override fun getById(accountId: AccountId): LegacyAccount? {
        val dto = accountManager.getById(accountId)
        return dto?.let { accountDataMapper.toDomain(it) }
    }

    override fun observeById(accountId: AccountId): Flow<LegacyAccount?> {
        return accountManager.observeById(accountId).map { account ->
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

    override fun getAccounts(): List<LegacyAccount> {
        return accountManager.getAccounts()
            .map { account ->
                accountDataMapper.toDomain(account)
            }
    }

    override fun getAccountsFlow(): Flow<List<LegacyAccount>> = getAll()

    override fun moveAccount(account: LegacyAccount, newPosition: Int) {
        accountManager.moveAccount(accountDataMapper.toDto(account), newPosition)
    }

    override fun saveAccount(account: LegacyAccount) {
        accountManager.saveAccount(accountDataMapper.toDto(account))
    }

    override fun updateSync(account: LegacyAccount) {
        saveAccount(account)
    }
}
