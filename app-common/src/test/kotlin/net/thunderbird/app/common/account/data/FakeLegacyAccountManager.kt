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

    override fun getAll(): Flow<List<LegacyAccount>> = accounts

    override fun getById(id: AccountId): Flow<LegacyAccount?> = accounts
        .map { list ->
            list.find { it.id == id }
        }

    override suspend fun update(account: LegacyAccount) {
        accountsState.update { currentList ->
            currentList.toMutableList().apply {
                removeIf { it.uuid == account.uuid }
                add(account)
            }
        }
    }

    override fun getAccounts(): List<LegacyAccount> {
        TODO("Not yet implemented")
    }

    override fun getAccountsFlow(): Flow<List<LegacyAccount>> {
        TODO("Not yet implemented")
    }

    override fun getAccount(accountUuid: String): LegacyAccount? {
        TODO("Not yet implemented")
    }

    override fun getAccountFlow(accountUuid: String): Flow<LegacyAccount?> {
        TODO("Not yet implemented")
    }

    override fun moveAccount(account: LegacyAccount, newPosition: Int) {
        TODO("Not yet implemented")
    }

    override fun saveAccount(account: LegacyAccount) {
        TODO("Not yet implemented")
    }
}
