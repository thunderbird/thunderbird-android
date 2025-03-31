package app.k9mail.feature.navigation.drawer.domain.usecase

import app.k9mail.legacy.account.AccountManager
import app.k9mail.legacy.account.AccountRemovedListener
import app.k9mail.legacy.account.AccountsChangeListener
import app.k9mail.legacy.account.LegacyAccount
import kotlinx.coroutines.flow.Flow

internal class FakeAccountManager(
    val recordedParameters: MutableList<String> = mutableListOf(),
    private val accounts: List<LegacyAccount> = emptyList(),
) : AccountManager {
    override fun getAccounts(): List<LegacyAccount> {
        TODO("Not yet implemented")
    }

    override fun getAccountsFlow(): Flow<List<LegacyAccount>> {
        TODO("Not yet implemented")
    }

    override fun getAccount(accountUuid: String): LegacyAccount? {
        recordedParameters.add(accountUuid)
        return accounts.find { it.uuid == accountUuid }
    }

    override fun getAccountFlow(accountUuid: String): Flow<LegacyAccount> {
        TODO("Not yet implemented")
    }

    override fun addAccountRemovedListener(listener: AccountRemovedListener) {
        TODO("Not yet implemented")
    }

    override fun moveAccount(account: LegacyAccount, newPosition: Int) {
        TODO("Not yet implemented")
    }

    override fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) {
        TODO("Not yet implemented")
    }

    override fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) {
        TODO("Not yet implemented")
    }

    override fun saveAccount(account: LegacyAccount) {
        TODO("Not yet implemented")
    }
}
