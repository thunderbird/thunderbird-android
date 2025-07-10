@file:JvmName("LocalSearchExtensions")

package com.fsck.k9.search

import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.search.LocalMessageSearch
import net.thunderbird.feature.search.SearchAccount

val LocalMessageSearch.isUnifiedInbox: Boolean
    get() = id == SearchAccount.UNIFIED_INBOX

val LocalMessageSearch.isNewMessages: Boolean
    get() = id == SearchAccount.NEW_MESSAGES

val LocalMessageSearch.isSingleAccount: Boolean
    get() = accountUuids.size == 1

val LocalMessageSearch.isSingleFolder: Boolean
    get() = isSingleAccount && folderIds.size == 1

@JvmName("getAccountsFromLocalSearch")
fun LocalMessageSearch.getAccounts(accountManager: AccountManager): List<LegacyAccount> {
    val accounts = accountManager.getAccounts()
    return if (searchAllAccounts()) {
        accounts
    } else {
        val searchAccountUuids = accountUuids.toSet()
        accounts.filter { it.uuid in searchAccountUuids }
    }
}

fun LocalMessageSearch.getAccountUuids(accountManager: AccountManager): List<String> {
    return getAccounts(accountManager).map { it.uuid }
}
