package com.fsck.k9.account

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

/** In-memory [LegacyAccountManager] that records every account passed to [updateSync]. */
class FakeLegacyAccountManager(
    initialAccounts: List<LegacyAccount> = emptyList(),
) : LegacyAccountManager {
    private val accounts = MutableStateFlow(initialAccounts)

    val updatedAccounts = mutableListOf<LegacyAccount>()

    override fun findAll(): List<LegacyAccount> = accounts.value

    override fun findById(accountId: AccountId): LegacyAccount? = accounts.value.firstOrNull { it.id == accountId }

    override fun observeAll(): Flow<List<LegacyAccount>> = accounts

    override fun observeById(accountId: AccountId): Flow<LegacyAccount?> =
        accounts.map { accounts -> accounts.firstOrNull { it.id == accountId } }

    override fun moveAccount(accountId: AccountId, newPosition: Int) = error("Not supported by this fake")

    override suspend fun update(account: LegacyAccount) = updateSync(account)

    override fun updateSync(account: LegacyAccount) {
        updatedAccounts.add(account)
        accounts.value = if (accounts.value.any { it.id == account.id }) {
            accounts.value.map { if (it.id == account.id) account else it }
        } else {
            accounts.value + account
        }
    }
}
