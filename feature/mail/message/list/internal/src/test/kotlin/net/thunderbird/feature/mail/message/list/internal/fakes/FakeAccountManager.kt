package net.thunderbird.feature.mail.message.list.internal.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.account.api.AccountManager
import net.thunderbird.feature.mail.account.api.BaseAccount

internal open class FakeAccountManager(
    private val accounts: List<BaseAccount>,
) : AccountManager<BaseAccount> {
    override fun getAccounts(): List<BaseAccount> = accounts

    override fun getAccountsFlow(): Flow<List<BaseAccount>> = flowOf(accounts)

    override fun getById(accountId: AccountId): BaseAccount? = accounts.firstOrNull { it.id == accountId }

    override fun getByIdFlow(accountId: AccountId): Flow<BaseAccount?> = flowOf(getById(accountId))

    override fun moveAccount(
        account: BaseAccount,
        newPosition: Int,
    ) = error("not implemented.")

    override fun saveAccount(account: BaseAccount) = Unit
}
