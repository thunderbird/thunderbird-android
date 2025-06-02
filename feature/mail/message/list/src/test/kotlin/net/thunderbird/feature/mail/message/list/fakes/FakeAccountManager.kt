package net.thunderbird.feature.mail.message.list.fakes

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.mail.account.api.AccountManager
import net.thunderbird.feature.mail.account.api.BaseAccount

internal class FakeAccountManager(
    private val accounts: List<BaseAccount>,
) : AccountManager<BaseAccount> {
    override fun getAccounts(): List<BaseAccount> = error("not implemented.")

    override fun getAccountsFlow(): Flow<List<BaseAccount>> = error("not implemented.")

    override fun getAccount(accountUuid: String): BaseAccount? = accounts.firstOrNull { it.uuid == accountUuid }

    override fun getAccountFlow(accountUuid: String): Flow<BaseAccount?> = error("not implemented.")

    override fun moveAccount(
        account: BaseAccount,
        newPosition: Int,
    ) = error("not implemented.")

    override fun saveAccount(account: BaseAccount) = Unit
}
