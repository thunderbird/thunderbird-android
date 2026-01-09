package net.thunderbird.feature.mail.message.list.internal.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

internal open class FakeLegacyAccountManager(
    private val accounts: List<LegacyAccount>,
) : LegacyAccountManager {
    override fun getAccounts(): List<LegacyAccount> = accounts

    override fun getAccountsFlow(): Flow<List<LegacyAccount>> = flowOf(accounts)

    override fun getAccount(accountUuid: String): LegacyAccount? = accounts.firstOrNull { it.uuid == accountUuid }

    override fun getAccountFlow(accountUuid: String): Flow<LegacyAccount?> = flowOf(getAccount(accountUuid))

    override fun moveAccount(
        account: LegacyAccount,
        newPosition: Int,
    ) = error("not implemented.")

    override fun saveAccount(account: LegacyAccount) = Unit
    override fun getAll(): Flow<List<LegacyAccount>> = flowOf(getAccounts())

    override fun getById(id: AccountId): Flow<LegacyAccount?> = flowOf(getAccount(id.toString()))

    override suspend fun update(account: LegacyAccount) = saveAccount(account)
}
