@file:JvmName("LocalSearchExtensions")

package com.fsck.k9.search

import app.k9mail.legacy.account.AccountManager
import app.k9mail.legacy.account.LegacyAccount
import app.k9mail.legacy.search.LocalSearch
import app.k9mail.legacy.search.SearchAccount

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
