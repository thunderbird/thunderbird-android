package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.android.account.AccountRemovedListener
import net.thunderbird.core.android.account.AccountsChangeListener
import net.thunderbird.core.android.account.LegacyAccountDto

internal class FakeAccountManager(
    val recordedParameters: MutableList<String> = mutableListOf(),
    private val accounts: List<LegacyAccountDto> = emptyList(),
) : AccountManager {
    override fun getAccounts(): List<LegacyAccountDto> {
        TODO("Not yet implemented")
    }

    override fun getAccountsFlow(): Flow<List<LegacyAccountDto>> {
        TODO("Not yet implemented")
    }

    override fun getAccount(accountUuid: String): LegacyAccountDto? {
        recordedParameters.add(accountUuid)
        return accounts.find { it.uuid == accountUuid }
    }

    override fun getAccountFlow(accountUuid: String): Flow<LegacyAccountDto> {
        TODO("Not yet implemented")
    }

    override fun addAccountRemovedListener(listener: AccountRemovedListener) {
        TODO("Not yet implemented")
    }

    override fun moveAccount(account: LegacyAccountDto, newPosition: Int) {
        TODO("Not yet implemented")
    }

    override fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) {
        TODO("Not yet implemented")
    }

    override fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) {
        TODO("Not yet implemented")
    }

    override fun saveAccount(account: LegacyAccountDto) {
        TODO("Not yet implemented")
    }
}
