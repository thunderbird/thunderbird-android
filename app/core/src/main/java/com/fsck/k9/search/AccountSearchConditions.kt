package com.fsck.k9.search

import com.fsck.k9.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.mail.FolderClass
import com.fsck.k9.search.SearchSpecification.Attribute
import com.fsck.k9.search.SearchSpecification.SearchCondition
import com.fsck.k9.search.SearchSpecification.SearchField

/**
 * Modify the supplied [LocalSearch] instance to limit the search to displayable folders.
 *
 * This method uses the current [folder display mode][Account.folderDisplayMode] to decide what folders to
 * include/exclude.
 */
fun LocalSearch.limitToDisplayableFolders(account: Account) {
    when (account.folderDisplayMode) {
        FolderMode.FIRST_CLASS -> {
            // Count messages in the INBOX and non-special first class folders
            and(SearchField.DISPLAY_CLASS, FolderClass.FIRST_CLASS.name, Attribute.EQUALS)
        }
        FolderMode.FIRST_AND_SECOND_CLASS -> {
            // Count messages in the INBOX and non-special first and second class folders
            and(SearchField.DISPLAY_CLASS, FolderClass.FIRST_CLASS.name, Attribute.EQUALS)

            // TODO: Create a proper interface for creating arbitrary condition trees
            val searchCondition = SearchCondition(
                SearchField.DISPLAY_CLASS,
                Attribute.EQUALS,
                FolderClass.SECOND_CLASS.name,
            )
            val root = conditions
            if (root.mRight != null) {
                root.mRight.or(searchCondition)
            } else {
                or(searchCondition)
            }
        }
        FolderMode.NOT_SECOND_CLASS -> {
            // Count messages in the INBOX and non-special non-second-class folders
            and(SearchField.DISPLAY_CLASS, FolderClass.SECOND_CLASS.name, Attribute.NOT_EQUALS)
        }
        FolderMode.ALL, FolderMode.NONE -> {
            // Count messages in the INBOX and non-special folders
        }
    }
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
        or(SearchCondition(SearchField.FOLDER, Attribute.EQUALS, inboxFolderId.toString()))
    }
}

private fun LocalSearch.excludeSpecialFolder(folderId: Long?) {
    if (folderId != null) {
        and(SearchField.FOLDER, folderId.toString(), Attribute.NOT_EQUALS)
    }
}
