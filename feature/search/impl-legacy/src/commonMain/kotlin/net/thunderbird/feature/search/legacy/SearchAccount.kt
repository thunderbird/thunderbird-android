package net.thunderbird.feature.search.legacy

import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.UnifiedAccountId
import net.thunderbird.feature.mail.folder.FolderType
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchAttribute

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
            val tmpSearch = LocalMessageSearch().apply {
                id = UnifiedAccountId
                type = LocalMessageSearchType.Unified(UnifiedFolderSelection.Special(FolderType.INBOX))
                // The ingrate field is used to identify the unified folders.
                and(MessageSearchField.INTEGRATE, "1", SearchAttribute.EQUALS)
            }

            return SearchAccount(
                id = UnifiedAccountId,
                search = tmpSearch,
                name = title,
                email = detail,
            )
        }
    }
}
