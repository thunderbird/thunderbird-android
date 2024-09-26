package app.k9mail.feature.navigation.drawer.domain.usecase

import app.k9mail.legacy.account.Account
import app.k9mail.legacy.account.AccountManager
import app.k9mail.legacy.account.AccountRemovedListener
import app.k9mail.legacy.account.AccountsChangeListener
import kotlinx.coroutines.flow.Flow

internal class FakeAccountManager(
    val recordedParameters: MutableList<String> = mutableListOf(),
    private val accounts: List<Account> = emptyList(),
) : AccountManager {
    override fun getAccounts(): List<Account> {
        TODO("Not yet implemented")
    }

    override fun getAccountsFlow(): Flow<List<Account>> {
        TODO("Not yet implemented")
    }

    override fun getAccount(accountUuid: String): Account? {
        recordedParameters.add(accountUuid)
        return accounts.find { it.uuid == accountUuid }
    }

    override fun getAccountFlow(accountUuid: String): Flow<Account> {
        TODO("Not yet implemented")
    }

    override fun addAccountRemovedListener(listener: AccountRemovedListener) {
        TODO("Not yet implemented")
    }

    override fun moveAccount(account: Account, newPosition: Int) {
        TODO("Not yet implemented")
    }

    override fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) {
        TODO("Not yet implemented")
    }

    override fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) {
        TODO("Not yet implemented")
    }

    override fun saveAccount(account: Account) {
        TODO("Not yet implemented")
    }
}
