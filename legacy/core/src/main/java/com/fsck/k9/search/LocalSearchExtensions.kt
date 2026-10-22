@file:JvmName("LocalSearchExtensions")

package com.fsck.k9.search

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.LocalMessageSearchType
import net.thunderbird.feature.search.legacy.SearchConditionTreeNode
import net.thunderbird.feature.search.legacy.UnifiedFolderSelection

val LocalMessageSearch.isUnified: Boolean
    get() = type is LocalMessageSearchType.Unified

/**
 * The role of the folders a unified search shows, or `null` if this isn't a unified search of a special folder.
 */
val LocalMessageSearch.unifiedFolderRole: FolderRole?
    get() = ((type as? LocalMessageSearchType.Unified)?.folder as? UnifiedFolderSelection.Special)?.role

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
): SearchConditionTreeNode? = conditionsForAccount { role ->
    account.folderIdFor(role, outboxFolderManager)
}

/**
 * Returns the ID of this account's folder with the given [role], or `null` if the account doesn't have one.
 */
fun LegacyAccount.folderIdFor(role: FolderRole, outboxFolderManager: OutboxFolderManager): Long? {
    return when (role) {
        FolderRole.INBOX -> inboxFolderId
        FolderRole.OUTBOX -> outboxFolderManager.getOutboxFolderIdSync(id).takeIf { it != -1L }
        FolderRole.SENT -> sentFolderId
        FolderRole.TRASH -> trashFolderId
        FolderRole.DRAFTS -> draftsFolderId
        FolderRole.ARCHIVE -> archiveFolderId
        FolderRole.SPAM -> spamFolderId
    }
}

/**
 * Returns the ID of each account's folder with the given [role], leaving out accounts that don't have one.
 */
fun LegacyAccountManager.folderIdsFor(
    role: FolderRole,
    outboxFolderManager: OutboxFolderManager,
): Map<AccountId, Long> {
    return findAll()
        .mapNotNull { account -> account.folderIdFor(role, outboxFolderManager)?.let { account.id to it } }
        .toMap()
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
