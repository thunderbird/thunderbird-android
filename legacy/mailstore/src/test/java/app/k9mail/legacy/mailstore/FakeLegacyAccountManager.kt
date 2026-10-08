package app.k9mail.legacy.mailstore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

internal class FakeLegacyAccountManager(
    private val accounts: List<LegacyAccount> = emptyList(),
) : LegacyAccountManager {
    override fun observeAll(): Flow<List<LegacyAccount>> = flowOf(accounts)
    override suspend fun update(account: LegacyAccount) = error("Not implemented")
    override fun updateSync(account: LegacyAccount) = error("Not implemented")
    override fun findAll(): List<LegacyAccount> = accounts
    override fun findById(accountId: AccountId): LegacyAccount? = accounts.find { it.id == accountId }
    override fun observeById(accountId: AccountId): Flow<LegacyAccount?> = flowOf(findById(accountId))
    override fun moveAccount(accountId: AccountId, newPosition: Int) = error("Not implemented")
}
