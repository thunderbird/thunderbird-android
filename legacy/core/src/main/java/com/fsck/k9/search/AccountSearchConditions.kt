package com.fsck.k9.search

import app.k9mail.legacy.account.Account
import app.k9mail.legacy.search.LocalSearch
import app.k9mail.legacy.search.api.SearchAttribute
import app.k9mail.legacy.search.api.SearchCondition
import app.k9mail.legacy.search.api.SearchField

/**
 * Modify the supplied [LocalSearch] instance to limit the search to displayable folders.
 */
fun LocalSearch.limitToDisplayableFolders() {
    and(SearchField.VISIBLE, "1", SearchAttribute.EQUALS)
}

/**
 * Modify the supplied [LocalSearch] instance to exclude special folders.
 *
 * Currently the following folders are excluded:
 *  - Trash
 *  - Drafts
 *  - Spam
 *  - Outbox
 *  - Sent
 *
 * The Inbox will always be included even if one of the special folders is configured to point to the Inbox.
 */
fun LocalSearch.excludeSpecialFolders(account: Account) {
    this.excludeSpecialFolder(account.trashFolderId)
    this.excludeSpecialFolder(account.draftsFolderId)
    this.excludeSpecialFolder(account.spamFolderId)
    this.excludeSpecialFolder(account.outboxFolderId)
    this.excludeSpecialFolder(account.sentFolderId)

    account.inboxFolderId?.let { inboxFolderId ->
        or(
            SearchCondition(
                SearchField.FOLDER,
                SearchAttribute.EQUALS,
                inboxFolderId.toString(),
            ),
        )
    }
}

private fun LocalSearch.excludeSpecialFolder(folderId: Long?) {
    if (folderId != null) {
        and(SearchField.FOLDER, folderId.toString(), SearchAttribute.NOT_EQUALS)
    }
}
