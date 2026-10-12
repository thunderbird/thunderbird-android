@file:JvmName("LocalSearchExtensions")

package com.fsck.k9.search

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.mail.folder.FolderType
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.LocalMessageSearchType
import net.thunderbird.feature.search.legacy.SearchConditionTreeNode
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

/**
 * Returns the conditions to run this search against [account]'s messages, or `null` if the account doesn't take part.
 *
 * @see LocalMessageSearch.conditionsForAccount
 */
fun LocalMessageSearch.conditionsFor(
    account: LegacyAccount,
    outboxFolderManager: OutboxFolderManager,
): SearchConditionTreeNode? = conditionsForAccount { folderType ->
    when (folderType) {
        FolderType.INBOX -> account.inboxFolderId
        FolderType.OUTBOX -> outboxFolderManager.getOutboxFolderIdSync(account.id).takeIf { it != -1L }
        FolderType.SENT -> account.sentFolderId
        FolderType.TRASH -> account.trashFolderId
        FolderType.DRAFTS -> account.draftsFolderId
        FolderType.ARCHIVE -> account.archiveFolderId
        FolderType.SPAM -> account.spamFolderId
        FolderType.REGULAR -> null
    }
}

@JvmName("getLegacyAccountsFromLocalSearch")
fun LocalMessageSearch.getLegacyAccounts(accountManager: LegacyAccountManager): List<LegacyAccount> {
    val accounts = accountManager.findAll()
    return if (searchAllAccounts()) {
        accounts
    } else {
        val searchAccountIds = accountIds.toSet()
        accounts.filter { it.id in searchAccountIds }
    }
}

fun LocalMessageSearch.getLegacyAccountUuids(accountManager: LegacyAccountManager): List<String> {
    return getLegacyAccounts(accountManager).map { it.id.toString() }
}
