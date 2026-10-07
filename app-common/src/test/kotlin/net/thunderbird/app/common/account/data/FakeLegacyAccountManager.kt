package net.thunderbird.app.common.account.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

internal class FakeLegacyAccountManager(
    initialAccounts: List<LegacyAccount> = emptyList(),
) : LegacyAccountManager {

    private val accountsState = MutableStateFlow(
        initialAccounts,
    )
    private val accounts: StateFlow<List<LegacyAccount>> = accountsState

    override fun observeAll(): Flow<List<LegacyAccount>> = accounts

    override fun findById(accountId: AccountId): LegacyAccount? {
        return accounts.value.find { it.id == accountId }
    }

    override fun observeById(accountId: AccountId): Flow<LegacyAccount?> = accounts
        .map { list ->
            list.find { it.id == accountId }
        }

    override fun moveAccount(accountId: AccountId, newPosition: Int) = Unit

    override suspend fun update(account: LegacyAccount) {
        accountsState.update { currentList ->
            currentList.toMutableList().apply {
                removeIf { it.id == account.id }
                add(account)
            }
        }
    }

    override fun updateSync(account: LegacyAccount) {
        accountsState.update { currentList ->
            currentList.toMutableList().apply {
                removeIf { it.id == account.id }
                add(account)
            }
        }
    }

    override fun findAll(): List<LegacyAccount> {
        return accounts.value
    }
}
