package net.thunderbird.app.common.account.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.storage.legacy.mapper.DefaultLegacyAccountWrapperDataMapper
import net.thunderbird.feature.mail.account.api.AccountManager

internal class DefaultLegacyAccountManager(
    private val accountManager: LegacyAccountDtoManager,
    private val accountDataMapper: DefaultLegacyAccountWrapperDataMapper,
) : LegacyAccountManager, AccountManager<LegacyAccount> {

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

    override fun getAccounts(): List<LegacyAccount> {
        return accountManager.getAccounts()
            .map { account ->
                accountDataMapper.toDomain(account)
            }
    }

    override fun getAccountsFlow(): Flow<List<LegacyAccount>> = getAll()

    override fun getAccount(accountUuid: String): LegacyAccount? {
        val dto = accountManager.getAccount(accountUuid)
        return dto?.let { accountDataMapper.toDomain(it) }
    }

    override fun getAccountFlow(accountUuid: String): Flow<LegacyAccount?> {
        return accountManager.getAccountFlow(accountUuid).map { account ->
            account?.let {
                accountDataMapper.toDomain(it)
            }
        }
    }

    override fun moveAccount(account: LegacyAccount, newPosition: Int) {
        accountManager.moveAccount(accountDataMapper.toDto(account), newPosition)
    }

    override fun saveAccount(account: LegacyAccount) {
        accountManager.saveAccount(accountDataMapper.toDto(account))
    }
}
