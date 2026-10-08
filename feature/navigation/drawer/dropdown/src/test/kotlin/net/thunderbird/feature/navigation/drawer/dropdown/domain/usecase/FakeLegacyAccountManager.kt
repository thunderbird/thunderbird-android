package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

internal class FakeLegacyAccountManager(
    val recordedParameters: MutableList<AccountId> = mutableListOf(),
    private val accounts: List<LegacyAccount> = emptyList(),
) : LegacyAccountManager {
    override fun findAll(): List<LegacyAccount> {
        TODO("Not yet implemented")
    }

    override fun findById(accountId: AccountId): LegacyAccount? {
        recordedParameters.add(accountId)
        return accounts.find { it.id == accountId }
    }

    override fun observeById(accountId: AccountId): Flow<LegacyAccount> {
        TODO("Not yet implemented")
    }

    override fun moveAccount(accountId: AccountId, newPosition: Int) {
        TODO("Not yet implemented")
    }

    override fun observeAll(): Flow<List<LegacyAccount>> {
        TODO("Not yet implemented")
    }

    override suspend fun update(account: LegacyAccount) {
        TODO("Not yet implemented")
    }

    override fun updateSync(account: LegacyAccount) {
        TODO("Not yet implemented")
    }
}
