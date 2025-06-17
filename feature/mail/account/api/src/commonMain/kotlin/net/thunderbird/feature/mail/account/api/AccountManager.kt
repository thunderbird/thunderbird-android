package net.thunderbird.feature.mail.account.api

import kotlinx.coroutines.flow.Flow

interface AccountManager<TAccount : BaseAccount> {
    fun getAccounts(): List<TAccount>
    fun getAccountsFlow(): Flow<List<TAccount>>
    fun getAccount(accountUuid: String): TAccount?
    fun getAccountFlow(accountUuid: String): Flow<TAccount?>
    fun moveAccount(account: TAccount, newPosition: Int)
    fun saveAccount(account: TAccount)
}
