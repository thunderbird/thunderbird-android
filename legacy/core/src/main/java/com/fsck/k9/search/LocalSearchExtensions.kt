@file:JvmName("LocalSearchExtensions")

package com.fsck.k9.search

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.mail.folder.FolderType
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.LocalMessageSearchType
import net.thunderbird.feature.search.legacy.UnifiedFolderSelection

val LocalMessageSearch.isUnified: Boolean
    get() = type is LocalMessageSearchType.Unified

val LocalMessageSearch.isUnifiedInbox: Boolean
    get() = (type as? LocalMessageSearchType.Unified)?.folder == UnifiedFolderSelection.Special(FolderType.INBOX)

val LocalMessageSearch.isNewMessages: Boolean
    get() = type == LocalMessageSearchType.NewMessages

val LocalMessageSearch.isSingleAccount: Boolean
    get() = accountIds.size == 1

val LocalMessageSearch.isSingleFolder: Boolean
    get() = isSingleAccount && folderIds.size == 1

@JvmName("getLegacyAccountsFromLocalSearch")
fun LocalMessageSearch.getLegacyAccounts(accountManager: LegacyAccountManager): List<LegacyAccount> {
    val accounts = accountManager.findAll()
    return if (searchAllAccounts()) {
        accounts
    } else {
        val searchAccountIds = accountIds.toSet()
        accounts.filter { it.id in searchAccountIds || it.id in searchAccountIds }
    }
}

fun LocalMessageSearch.getLegacyAccountUuids(accountManager: LegacyAccountManager): List<String> {
    return getLegacyAccounts(accountManager).map { it.id.toString() }
}
