package com.fsck.k9.search

import com.fsck.k9.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.mail.FolderClass
import com.fsck.k9.search.SearchSpecification.Attribute
import com.fsck.k9.search.SearchSpecification.SearchCondition
import com.fsck.k9.search.SearchSpecification.SearchField

class AccountSearchConditions {
    /**
     * Modify the supplied [LocalSearch] instance to limit the search to displayable folders.
     *
     * This method uses the current folder display mode to decide what folders to include/exclude.
     *
     * @param search
     * The `LocalSearch` instance to modify.
     *
     * @see .getFolderDisplayMode
     */
    fun limitToDisplayableFolders(account: Account, search: LocalSearch) {
        val displayMode = account.folderDisplayMode

        when (displayMode) {
            FolderMode.FIRST_CLASS -> {
                // Count messages in the INBOX and non-special first class folders
                search.and(SearchField.DISPLAY_CLASS, FolderClass.FIRST_CLASS.name, Attribute.EQUALS)
            }
            FolderMode.FIRST_AND_SECOND_CLASS -> {
                // Count messages in the INBOX and non-special first and second class folders
                search.and(SearchField.DISPLAY_CLASS, FolderClass.FIRST_CLASS.name, Attribute.EQUALS)

                // TODO: Create a proper interface for creating arbitrary condition trees
                val searchCondition = SearchCondition(
                    SearchField.DISPLAY_CLASS, Attribute.EQUALS, FolderClass.SECOND_CLASS.name
                )
                val root = search.conditions
                if (root.mRight != null) {
                    root.mRight.or(searchCondition)
                } else {
                    search.or(searchCondition)
                }
            }
            FolderMode.NOT_SECOND_CLASS -> {
                // Count messages in the INBOX and non-special non-second-class folders
                search.and(SearchField.DISPLAY_CLASS, FolderClass.SECOND_CLASS.name, Attribute.NOT_EQUALS)
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
     *
     *  * Trash
     *  * Drafts
     *  * Spam
     *  * Outbox
     *  * Sent
     *
     * The Inbox will always be included even if one of the special folders is configured to point
     * to the Inbox.
     *
     * @param search
     * The `LocalSearch` instance to modify.
     */
    fun excludeSpecialFolders(account: Account, search: LocalSearch) {
        excludeSpecialFolder(search, account.trashFolderId)
        excludeSpecialFolder(search, account.draftsFolderId)
        excludeSpecialFolder(search, account.spamFolderId)
        excludeSpecialFolder(search, account.outboxFolderId)
        excludeSpecialFolder(search, account.sentFolderId)
        account.inboxFolderId?.let { inboxFolderId ->
            search.or(SearchCondition(SearchField.FOLDER, Attribute.EQUALS, inboxFolderId.toString()))
        }
    }

    /**
     * Modify the supplied [LocalSearch] instance to exclude "unwanted" folders.
     *
     * Currently the following folders are excluded:
     *
     *  * Trash
     *  * Spam
     *  * Outbox
     *
     * The Inbox will always be included even if one of the special folders is configured to point
     * to the Inbox.
     *
     * @param search
     * The `LocalSearch` instance to modify.
     */
    fun excludeUnwantedFolders(account: Account, search: LocalSearch) {
        excludeSpecialFolder(search, account.trashFolderId)
        excludeSpecialFolder(search, account.spamFolderId)
        excludeSpecialFolder(search, account.outboxFolderId)
        account.inboxFolderId?.let { inboxFolderId ->
            search.or(SearchCondition(SearchField.FOLDER, Attribute.EQUALS, inboxFolderId.toString()))
        }
    }

    private fun excludeSpecialFolder(search: LocalSearch, folderId: Long?) {
        if (folderId != null) {
            search.and(SearchField.FOLDER, folderId.toString(), Attribute.NOT_EQUALS)
        }
    }
}
