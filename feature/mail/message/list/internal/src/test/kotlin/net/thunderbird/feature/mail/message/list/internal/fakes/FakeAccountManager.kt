package net.thunderbird.feature.mail.message.list.internal.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.feature.mail.account.api.AccountManager
import net.thunderbird.feature.mail.account.api.BaseAccount

internal open class FakeAccountManager(
    private val accounts: List<BaseAccount>,
) : AccountManager<BaseAccount> {
    override fun getAccounts(): List<BaseAccount> = accounts

    override fun getAccountsFlow(): Flow<List<BaseAccount>> = flowOf(accounts)

    override fun getAccount(accountUuid: String): BaseAccount? = accounts.firstOrNull { it.uuid == accountUuid }

    override fun getAccountFlow(accountUuid: String): Flow<BaseAccount?> = flowOf(getAccount(accountUuid))

    override fun moveAccount(
        account: BaseAccount,
        newPosition: Int,
    ) = error("not implemented.")

    override fun saveAccount(account: BaseAccount) = Unit
}
