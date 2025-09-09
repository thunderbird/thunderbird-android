@file:JvmName("LocalSearchExtensions")

package com.fsck.k9.search

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.mail.account.api.AccountManager
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.SearchAccount
import net.thunderbird.core.android.account.AccountManager as LegacyAccountManager

val LocalMessageSearch.isUnifiedFolders: Boolean
    get() = id == SearchAccount.UNIFIED_FOLDERS

val LocalMessageSearch.isNewMessages: Boolean
    get() = id == SearchAccount.NEW_MESSAGES

val LocalMessageSearch.isSingleAccount: Boolean
    get() = accountUuids.size == 1

val LocalMessageSearch.isSingleFolder: Boolean
    get() = isSingleAccount && folderIds.size == 1

@Deprecated("Use getLegacyAccounts instead")
@JvmName("getAccountsFromLocalSearch")
fun LocalMessageSearch.getAccounts(accountManager: LegacyAccountManager): List<LegacyAccountDto> {
    val accounts = accountManager.getAccounts()
    return if (searchAllAccounts()) {
        accounts
    } else {
        val searchAccountUuids = accountUuids.toSet()
        accounts.filter { it.uuid in searchAccountUuids }
    }
}

@JvmName("getLegacyAccountsFromLocalSearch")
fun LocalMessageSearch.getLegacyAccounts(accountManager: AccountManager<LegacyAccount>): List<LegacyAccount> {
    val accounts = accountManager.getAccounts()
    return if (searchAllAccounts()) {
        accounts
    } else {
        val searchAccountUuids = accountUuids.toSet()
        accounts.filter { it.uuid in searchAccountUuids }
    }
}

fun LocalMessageSearch.getLegacyAccountUuids(accountManager: AccountManager<LegacyAccount>): List<String> {
    return getLegacyAccounts(accountManager).map { it.uuid }
}
