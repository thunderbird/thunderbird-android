package net.thunderbird.feature.mail.message.list.internal.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

internal open class FakeLegacyAccountManager(
    accounts: List<LegacyAccount> = emptyList(),
) : LegacyAccountManager {
    @get:JvmName("getMutableAccounts")
    val accounts: MutableList<LegacyAccount> = accounts.toMutableList()
    val getByIdFlowCalls = mutableListOf<AccountId>()
    val savedAccounts = mutableListOf<LegacyAccount>()

    override fun findAll(): List<LegacyAccount> = accounts

    override fun findById(accountId: AccountId): LegacyAccount? = accounts.firstOrNull { it.id == accountId }

    override fun observeById(accountId: AccountId): Flow<LegacyAccount?> {
        getByIdFlowCalls += accountId
        return flowOf(findById(accountId))
    }

    override fun moveAccount(accountId: AccountId, newPosition: Int) {
        TODO("Not yet implemented")
    }

    override fun observeAll(): Flow<List<LegacyAccount>> = flowOf(findAll())

    override suspend fun update(account: LegacyAccount) = updateSync(account)

    override fun updateSync(account: LegacyAccount) {
        savedAccounts += account
    }
}
