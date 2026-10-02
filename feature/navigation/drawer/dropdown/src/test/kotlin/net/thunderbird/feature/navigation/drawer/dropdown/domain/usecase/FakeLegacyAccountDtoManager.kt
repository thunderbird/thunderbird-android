package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.AccountRemovedListener
import net.thunderbird.core.android.account.AccountsChangeListener
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.feature.account.AccountId

internal class FakeLegacyAccountDtoManager(
    val recordedParameters: MutableList<AccountId> = mutableListOf(),
    private val accounts: List<LegacyAccountDto> = emptyList(),
) : LegacyAccountDtoManager {
    override fun getAccounts(): List<LegacyAccountDto> {
        TODO("Not yet implemented")
    }

    override fun getAccountsFlow(): Flow<List<LegacyAccountDto>> {
        TODO("Not yet implemented")
    }

    override fun getById(accountId: AccountId): LegacyAccountDto? {
        recordedParameters.add(accountId)
        return accounts.find { it.id == accountId }
    }

    override fun observeById(accountId: AccountId): Flow<LegacyAccountDto> {
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
