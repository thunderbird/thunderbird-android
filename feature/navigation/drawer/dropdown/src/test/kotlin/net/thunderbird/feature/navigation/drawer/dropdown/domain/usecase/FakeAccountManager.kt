package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.android.account.AccountRemovedListener
import net.thunderbird.core.android.account.AccountsChangeListener
import net.thunderbird.core.android.account.LegacyAccount

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
