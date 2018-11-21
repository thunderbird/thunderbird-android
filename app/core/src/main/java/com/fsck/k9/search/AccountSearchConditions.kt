package com.fsck.k9.search

import com.fsck.k9.Account
import com.fsck.k9.BaseAccount
import com.fsck.k9.mail.Folder

class AccountSearchConditions {
    /**
     * Modify the supplied [LocalSearch] instance to limit the search to displayable folders.
     *
     *
     *
     * This method uses the current folder display mode to decide what folders to include/exclude.
     *
     *
     * @param search
     * The `LocalSearch` instance to modify.
     *
     * @see .getFolderDisplayMode
     */
    fun limitToDisplayableFolders(account: Account, search: LocalSearch) {
        val displayMode = account.folderDisplayMode

        when (displayMode) {
            Account.FolderMode.FIRST_CLASS -> {
                // Count messages in the INBOX and non-special first class folders
                search.and(SearchSpecification.SearchField.DISPLAY_CLASS, Folder.FolderClass.FIRST_CLASS.name,
                        SearchSpecification.Attribute.EQUALS)
            }
            Account.FolderMode.FIRST_AND_SECOND_CLASS -> {
                // Count messages in the INBOX and non-special first and second class folders
                search.and(SearchSpecification.SearchField.DISPLAY_CLASS, Folder.FolderClass.FIRST_CLASS.name,
                        SearchSpecification.Attribute.EQUALS)

                // TODO: Create a proper interface for creating arbitrary condition trees
                val searchCondition = SearchSpecification.SearchCondition(SearchSpecification.SearchField.DISPLAY_CLASS,
                        SearchSpecification.Attribute.EQUALS, Folder.FolderClass.SECOND_CLASS.name)
                val root = search.conditions
                if (root.mRight != null) {
                    root.mRight.or(searchCondition)
                } else {
                    search.or(searchCondition)
                }
            }
            Account.FolderMode.NOT_SECOND_CLASS -> {
                // Count messages in the INBOX and non-special non-second-class folders
                search.and(SearchSpecification.SearchField.DISPLAY_CLASS, Folder.FolderClass.SECOND_CLASS.name,
                        SearchSpecification.Attribute.NOT_EQUALS)
            }
            Account.FolderMode.ALL -> {
            }// Count messages in the INBOX and non-special folders
            else -> {
            }
        }
    }

    /**
     * Modify the supplied [LocalSearch] instance to exclude special folders.
     *
     *
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
     *
     * @param search
     * The `LocalSearch` instance to modify.
     */
    fun excludeSpecialFolders(account: Account, search: LocalSearch) {
        excludeSpecialFolder(search, account.trashFolder)
        excludeSpecialFolder(search, account.draftsFolder)
        excludeSpecialFolder(search, account.spamFolder)
        excludeSpecialFolder(search, account.outboxFolder)
        excludeSpecialFolder(search, account.sentFolder)
        search.or(SearchSpecification.SearchCondition(SearchSpecification.SearchField.FOLDER, SearchSpecification.Attribute.EQUALS, account.getInboxFolder()))
    }

    /**
     * Modify the supplied [LocalSearch] instance to exclude "unwanted" folders.
     *
     *
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
     *
     * @param search
     * The `LocalSearch` instance to modify.
     */
    fun excludeUnwantedFolders(account: Account, search: LocalSearch) {
        excludeSpecialFolder(search, account.trashFolder)
        excludeSpecialFolder(search, account.spamFolder)
        excludeSpecialFolder(search, account.outboxFolder)
        search.or(SearchSpecification.SearchCondition(SearchSpecification.SearchField.FOLDER, SearchSpecification.Attribute.EQUALS, account.inboxFolder))
    }

    private fun excludeSpecialFolder(search: LocalSearch, folderServerId: String?) {
        if (folderServerId != null) {
            search.and(SearchSpecification.SearchField.FOLDER, folderServerId, SearchSpecification.Attribute.NOT_EQUALS)
        }
    }

    fun createUnreadSearch(account: BaseAccount, searchTitle: String): LocalSearch {
        val search: LocalSearch
        if (account is SearchAccount) {
            search = account.relatedSearch.clone()
            search.name = searchTitle
        } else {
            search = LocalSearch(searchTitle)
            search.addAccountUuid(account.uuid)

            val realAccount = account as Account
            excludeSpecialFolders(realAccount, search)
            limitToDisplayableFolders(realAccount, search)
        }

        search.and(SearchSpecification.SearchField.READ, "1", SearchSpecification.Attribute.NOT_EQUALS)

        return search
    }
}