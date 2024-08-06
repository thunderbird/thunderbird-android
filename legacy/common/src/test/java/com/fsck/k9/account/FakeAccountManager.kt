package com.fsck.k9.account

import app.k9mail.legacy.account.Account
import app.k9mail.legacy.account.AccountManager
import app.k9mail.legacy.account.AccountRemovedListener
import app.k9mail.legacy.account.AccountsChangeListener
import kotlinx.coroutines.flow.Flow

class FakeAccountManager(
    private val accounts: MutableMap<String, Account> = mutableMapOf(),
    private val isFailureOnSave: Boolean = false,
) : AccountManager {

    override fun getAccounts(): List<Account> = accounts.values.toList()

    override fun getAccountsFlow(): Flow<List<Account>> {
        TODO("Not yet implemented")
    }

    override fun getAccount(accountUuid: String): Account? = accounts[accountUuid]

    override fun getAccountFlow(accountUuid: String): Flow<Account> {
        TODO("Not yet implemented")
    }

    override fun addAccountRemovedListener(listener: AccountRemovedListener) {
        TODO("Not yet implemented")
    }

    override fun moveAccount(account: Account, newPosition: Int) {
        TODO("Not yet implemented")
    }

    override fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) {
        TODO("Not yet implemented")
    }

    override fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) {
        TODO("Not yet implemented")
    }

    @Suppress("TooGenericExceptionThrown")
    override fun saveAccount(account: Account) {
        if (isFailureOnSave) {
            throw Exception("FakeAccountManager.saveAccount() failed")
        }
        accounts[account.uuid] = account
    }
}
