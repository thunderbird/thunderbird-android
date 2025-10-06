package net.thunderbird.feature.search.legacy

import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchAttribute

/**
 * This class is basically a wrapper around a LocalSearch. It allows to expose it as an account.
 * This is a meta-account containing all the messages that match the search.
 */
class SearchAccount(
    val id: String,
    search: LocalMessageSearch,
    override val name: String,
    override val email: String,
) : BaseAccount {
    /**
     * Returns the ID of this `SearchAccount` instance.
     *
     * This isn't really a UUID. But since we don't expose this value to other apps and we only use the account UUID
     * as opaque string (e.g. as key in a `Map`) we're fine.
     *
     * Using a constant string is necessary to identify the same search account even when the corresponding
     * [SearchAccount] object has been recreated.
     */
    override val uuid: String = id

    val relatedSearch: LocalMessageSearch = search

    companion object {
        const val UNIFIED_INBOX = "unified_inbox"
        const val NEW_MESSAGES = "new_messages"

        @JvmStatic
        fun createUnifiedInboxAccount(
            unifiedInboxTitle: String,
            unifiedInboxDetail: String,
        ): SearchAccount {
            val tmpSearch = LocalMessageSearch().apply {
                id = UNIFIED_INBOX
                and(MessageSearchField.INTEGRATE, "1", SearchAttribute.EQUALS)
            }

            return SearchAccount(
                id = UNIFIED_INBOX,
                search = tmpSearch,
                name = unifiedInboxTitle,
                email = unifiedInboxDetail,
            )
        }
    }
}
