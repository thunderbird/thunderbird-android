@file:JvmName("LocalSearchExtensions")

package com.fsck.k9.search

import com.fsck.k9.Account
import com.fsck.k9.Preferences

val LocalSearch.isUnifiedInbox: Boolean
    get() = id == SearchAccount.UNIFIED_INBOX

val LocalSearch.isNewMessages: Boolean
    get() = id == SearchAccount.NEW_MESSAGES

val LocalSearch.isSingleAccount: Boolean
    get() = accountUuids.size == 1

val LocalSearch.isSingleFolder: Boolean
    get() = isSingleAccount && folderIds.size == 1

@JvmName("getAccountsFromLocalSearch")
fun LocalSearch.getAccounts(preferences: Preferences): List<Account> {
    val accounts = preferences.accounts
    return if (searchAllAccounts()) {
        accounts
    } else {
        val searchAccountUuids = accountUuids.toSet()
        accounts.filter { it.uuid in searchAccountUuids }
    }
}

fun LocalSearch.getAccountUuids(preferences: Preferences): List<String> {
    return getAccounts(preferences).map { it.uuid }
}
