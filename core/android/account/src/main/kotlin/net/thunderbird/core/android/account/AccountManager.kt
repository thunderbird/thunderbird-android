package net.thunderbird.core.android.account

import kotlinx.coroutines.flow.Flow

interface AccountManager {
    fun getAccounts(): List<LegacyAccount>
    fun getAccountsFlow(): Flow<List<LegacyAccount>>
    fun getAccount(accountUuid: String): LegacyAccount?
    fun getAccountFlow(accountUuid: String): Flow<LegacyAccount?>
    fun addAccountRemovedListener(listener: AccountRemovedListener)
    fun moveAccount(account: LegacyAccount, newPosition: Int)
    fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener)
    fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener)
    fun saveAccount(account: LegacyAccount)
}
