package net.thunderbird.app.common.account.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import net.thunderbird.core.android.account.LegacyAccountWrapper
import net.thunderbird.core.android.account.LegacyAccountWrapperManager

internal class FakeLegacyAccountWrapperManager(
    initialAccounts: List<LegacyAccountWrapper> = emptyList(),
) : LegacyAccountWrapperManager {

    private val accountsState = MutableStateFlow<List<LegacyAccountWrapper>>(
        initialAccounts,
    )
    private val accounts: StateFlow<List<LegacyAccountWrapper>> = accountsState

    override fun getAll(): Flow<List<LegacyAccountWrapper>> = accounts

    override fun getById(id: String): Flow<LegacyAccountWrapper?> = accounts
        .map { list ->
            list.find { it.uuid == id }
        }

    override suspend fun update(account: LegacyAccountWrapper) {
        accountsState.update { currentList ->
            currentList.toMutableList().apply {
                removeIf { it.uuid == account.uuid }
                add(account)
            }
        }
    }
}
