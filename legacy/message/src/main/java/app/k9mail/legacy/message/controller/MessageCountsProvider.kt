package app.k9mail.legacy.message.controller

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.SearchAccount

interface MessageCountsProvider {
    fun getMessageCounts(account: LegacyAccount): MessageCounts
    fun getMessageCounts(searchAccount: SearchAccount): MessageCounts
    fun getMessageCounts(search: LocalMessageSearch): MessageCounts
    fun getMessageCountsFlow(search: LocalMessageSearch): Flow<MessageCounts>
    fun getUnreadMessageCount(account: LegacyAccount, folderId: Long): Int
}

data class MessageCounts(val unread: Int, val starred: Int)
