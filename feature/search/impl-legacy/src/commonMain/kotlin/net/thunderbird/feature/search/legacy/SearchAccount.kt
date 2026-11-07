package net.thunderbird.feature.search.legacy

import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.UnifiedAccountId
import net.thunderbird.feature.mail.folder.FolderRole

/**
 * This class is basically a wrapper around a LocalSearch. It allows to expose it as an account.
 * This is a meta-account containing all the messages that match the search.
 */
class SearchAccount(
    val id: AccountId,
    search: LocalMessageSearch,
    val name: String,
    val email: String,
) {
    val relatedSearch: LocalMessageSearch = search

    companion object {
        @JvmStatic
        fun createUnifiedInboxSearch(
            title: String,
            detail: String,
        ): SearchAccount {
            val tmpSearch = LocalMessageSearch.createUnifiedSearch(FolderRole.INBOX)

            return SearchAccount(
                id = UnifiedAccountId,
                search = tmpSearch,
                name = title,
                email = detail,
            )
        }
    }
}
