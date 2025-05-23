@file:JvmName("LocalSearchExtensions")

package com.fsck.k9.search

import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.search.LocalSearch
import net.thunderbird.feature.search.SearchAccount

val LocalSearch.isUnifiedInbox: Boolean
    get() = id == SearchAccount.UNIFIED_INBOX

val LocalSearch.isNewMessages: Boolean
    get() = id == SearchAccount.NEW_MESSAGES

val LocalSearch.isSingleAccount: Boolean
    get() = accountUuids.size == 1

val LocalSearch.isSingleFolder: Boolean
    get() = isSingleAccount && folderIds.size == 1

@JvmName("getAccountsFromLocalSearch")
fun LocalSearch.getAccounts(accountManager: AccountManager): List<LegacyAccount> {
    val accounts = accountManager.getAccounts()
    return if (searchAllAccounts()) {
        accounts
    } else {
        val searchAccountUuids = accountUuids.toSet()
        accounts.filter { it.uuid in searchAccountUuids }
    }
}

fun LocalSearch.getAccountUuids(accountManager: AccountManager): List<String> {
    return getAccounts(accountManager).map { it.uuid }
}
