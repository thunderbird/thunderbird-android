package com.fsck.k9.controller

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.AccountStats
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.search.AccountSearchConditions
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount

interface AccountStatsCollector {
    @Throws(MessagingException::class)
    fun getStats(account: Account): AccountStats?

    fun getSearchAccountStats(searchAccount: SearchAccount): AccountStats
}

internal class DefaultAccountStatsCollector(
        private val context: Context,
        private val accountSearchConditions: AccountSearchConditions
) : AccountStatsCollector {
    private val preferences = Preferences.getPreferences(context)


    override fun getStats(account: Account): AccountStats? {
        if (!account.isAvailable(context)) {
            return null
        }

        val localStore = account.localStore

        val search = LocalSearch()
        accountSearchConditions.excludeSpecialFolders(account, search)
        accountSearchConditions.limitToDisplayableFolders(account, search)

        val accountStats = localStore.getAccountStats(search)
        if (K9.measureAccounts()) {
            accountStats.size = localStore.size
        }

        return accountStats
    }

    override fun getSearchAccountStats(searchAccount: SearchAccount): AccountStats {
        val search = searchAccount.relatedSearch
        val accounts = getAccountsFromLocalSearch(search)

        val aggregatedAccountStats = AccountStats()
        for (account in accounts) {
            val accountStats = account.localStore.getAccountStats(search)
            aggregatedAccountStats.unreadMessageCount += accountStats.unreadMessageCount
            aggregatedAccountStats.flaggedMessageCount += accountStats.flaggedMessageCount
        }

        return aggregatedAccountStats
    }

    private fun getAccountsFromLocalSearch(search: LocalSearch): List<Account> {
        return if (search.searchAllAccounts()) {
            preferences.accounts
        } else {
            preferences.accounts.filter { it.uuid in search.accountUuids }
        }
    }
}
