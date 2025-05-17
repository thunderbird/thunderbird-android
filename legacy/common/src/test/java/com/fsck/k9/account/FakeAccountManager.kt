package com.fsck.k9.account

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.android.account.AccountRemovedListener
import net.thunderbird.core.android.account.AccountsChangeListener
import net.thunderbird.core.android.account.LegacyAccount

class FakeAccountManager(
    private val accounts: MutableMap<String, LegacyAccount> = mutableMapOf(),
    private val isFailureOnSave: Boolean = false,
) : AccountManager {

    override fun getAccounts(): List<LegacyAccount> = accounts.values.toList()

    override fun getAccountsFlow(): Flow<List<LegacyAccount>> {
        TODO("Not yet implemented")
    }

    override fun getAccount(accountUuid: String): LegacyAccount? = accounts[accountUuid]

    override fun getAccountFlow(accountUuid: String): Flow<LegacyAccount> {
        TODO("Not yet implemented")
    }

    override fun addAccountRemovedListener(listener: AccountRemovedListener) {
        TODO("Not yet implemented")
    }

    override fun moveAccount(account: LegacyAccount, newPosition: Int) {
        TODO("Not yet implemented")
    }

    override fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) {
        TODO("Not yet implemented")
    }

    override fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) {
        TODO("Not yet implemented")
    }

    @Suppress("TooGenericExceptionThrown")
    override fun saveAccount(account: LegacyAccount) {
        if (isFailureOnSave) {
            throw Exception("FakeAccountManager.saveAccount() failed")
        }
        accounts[account.uuid] = account
    }
}
