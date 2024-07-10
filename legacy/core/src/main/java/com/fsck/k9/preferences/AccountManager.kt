package com.fsck.k9.preferences

import com.fsck.k9.Account
import com.fsck.k9.AccountRemovedListener
import com.fsck.k9.AccountsChangeListener
import kotlinx.coroutines.flow.Flow

interface AccountManager {
    fun getAccounts(): List<Account>
    fun getAccountsFlow(): Flow<List<Account>>
    fun getAccount(accountUuid: String): Account?
    fun getAccountFlow(accountUuid: String): Flow<Account>
    fun addAccountRemovedListener(listener: AccountRemovedListener)
    fun moveAccount(account: Account, newPosition: Int)
    fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener)
    fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener)
    fun saveAccount(account: Account)
}
