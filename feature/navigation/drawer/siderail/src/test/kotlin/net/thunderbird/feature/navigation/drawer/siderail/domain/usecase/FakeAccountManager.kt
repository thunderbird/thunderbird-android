package net.thunderbird.feature.navigation.drawer.siderail.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.core.android.account.AccountRemovedListener
import net.thunderbird.core.android.account.AccountsChangeListener
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager

internal class FakeAccountManager(
    val recordedParameters: MutableList<String> = mutableListOf(),
    private val accounts: List<LegacyAccountDto> = emptyList(),
) : LegacyAccountDtoManager {
    val removedListeners: MutableList<AccountRemovedListener> = mutableListOf()
    val accountsChangeListeners: MutableList<AccountsChangeListener> = mutableListOf()
    val movedAccounts: MutableList<Pair<LegacyAccountDto, Int>> = mutableListOf()
    val savedAccounts: MutableList<LegacyAccountDto> = mutableListOf()

    override fun getAccounts(): List<LegacyAccountDto> {
        return accounts
    }

    override fun getAccountsFlow(): Flow<List<LegacyAccountDto>> {
        return flowOf(accounts)
    }

    override fun getAccount(accountUuid: String): LegacyAccountDto? {
        recordedParameters.add(accountUuid)
        return accounts.find { it.uuid == accountUuid }
    }

    override fun getAccountFlow(accountUuid: String): Flow<LegacyAccountDto?> {
        return flowOf(getAccount(accountUuid))
    }

    override fun addAccountRemovedListener(listener: AccountRemovedListener) {
        removedListeners.add(listener)
    }

    override fun moveAccount(account: LegacyAccountDto, newPosition: Int) {
        movedAccounts.add(account to newPosition)
    }

    override fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) {
        accountsChangeListeners.add(accountsChangeListener)
    }

    override fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) {
        accountsChangeListeners.remove(accountsChangeListener)
    }

    override fun saveAccount(account: LegacyAccountDto) {
        savedAccounts.add(account)
    }
}
