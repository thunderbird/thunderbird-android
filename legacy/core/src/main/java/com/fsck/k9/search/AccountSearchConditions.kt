package com.fsck.k9.search

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchAttribute
import net.thunderbird.feature.search.legacy.api.SearchCondition

/**
 * Modify the supplied [LocalMessageSearch] instance to limit the search to displayable folders.
 */
fun LocalMessageSearch.limitToDisplayableFolders() {
    and(
        MessageSearchField.VISIBLE,
        "1",
        SearchAttribute.EQUALS,
    )
}

/**
 * Modify the supplied [LocalMessageSearch] instance to exclude special folders.
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
fun LocalMessageSearch.excludeSpecialFolders(account: LegacyAccount) {
    this.excludeSpecialFolder(account.trashFolderId)
    this.excludeSpecialFolder(account.draftsFolderId)
    this.excludeSpecialFolder(account.spamFolderId)
    this.excludeSpecialFolder(account.outboxFolderId)
    this.excludeSpecialFolder(account.sentFolderId)

    account.inboxFolderId?.let { inboxFolderId ->
        or(
            SearchCondition(
                MessageSearchField.FOLDER,
                SearchAttribute.EQUALS,
                inboxFolderId.toString(),
            ),
        )
    }
}

private fun LocalMessageSearch.excludeSpecialFolder(folderId: Long?) {
    if (folderId != null) {
        and(
            MessageSearchField.FOLDER,
            folderId.toString(),
            SearchAttribute.NOT_EQUALS,
        )
    }
}
