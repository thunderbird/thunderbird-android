package com.fsck.k9.account

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

class FakeLegacyAccountManager(
    private val accounts: MutableMap<AccountId, LegacyAccount> = mutableMapOf(),
    private val isFailureOnSave: Boolean = false,
) : LegacyAccountManager {

    override fun findAll(): List<LegacyAccount> = accounts.values.toList()

    override fun observeAll(): Flow<List<LegacyAccount>> {
        TODO("Not yet implemented")
    }

    override fun findById(accountId: AccountId): LegacyAccount? = accounts[accountId]

    override fun observeById(accountId: AccountId): Flow<LegacyAccount?> {
        TODO("Not yet implemented")
    }

    override fun moveAccount(accountId: AccountId, newPosition: Int) {
        TODO("Not yet implemented")
    }

    @Suppress("TooGenericExceptionThrown")
    override suspend fun update(account: LegacyAccount) {
        updateSync(account)
    }

    @Suppress("TooGenericExceptionThrown")
    override fun updateSync(account: LegacyAccount) {
        if (isFailureOnSave) {
            throw Exception("FakeAccountManager.updateSync() failed")
        }
        accounts[account.id] = account
    }
}
