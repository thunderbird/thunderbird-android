package net.thunderbird.feature.mail.message.list.internal.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

internal open class FakeLegacyAccountManager(
    private val accounts: List<LegacyAccount>,
) : LegacyAccountManager {
    val getByIdFlowCalls = mutableListOf<AccountId>()
    val savedAccounts = mutableListOf<LegacyAccount>()

    override fun getAccounts(): List<LegacyAccount> = accounts

    override fun getAccountsFlow(): Flow<List<LegacyAccount>> = flowOf(accounts)

    override fun getById(accountId: AccountId): LegacyAccount? = accounts.firstOrNull { it.id == accountId }

    override fun getByIdFlow(accountId: AccountId): Flow<LegacyAccount?> {
        getByIdFlowCalls += accountId
        return flowOf(getById(accountId))
    }

    override fun moveAccount(
        account: LegacyAccount,
        newPosition: Int,
    ) = error("not implemented.")

    override fun saveAccount(account: LegacyAccount) {
        savedAccounts += account
    }
    override fun getAll(): Flow<List<LegacyAccount>> = flowOf(getAccounts())

    override suspend fun update(account: LegacyAccount) = saveAccount(account)

    override fun updateSync(account: LegacyAccount) = saveAccount(account)
}
