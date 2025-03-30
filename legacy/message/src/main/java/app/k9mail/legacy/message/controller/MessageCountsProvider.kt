package app.k9mail.legacy.message.controller

import app.k9mail.legacy.account.LegacyAccount
import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.search.LocalSearch
import net.thunderbird.feature.search.SearchAccount

interface MessageCountsProvider {
    fun getMessageCounts(account: LegacyAccount): MessageCounts
    fun getMessageCounts(searchAccount: SearchAccount): MessageCounts
    fun getMessageCounts(search: LocalSearch): MessageCounts
    fun getMessageCountsFlow(search: LocalSearch): Flow<MessageCounts>
    fun getUnreadMessageCount(account: LegacyAccount, folderId: Long): Int
}

data class MessageCounts(val unread: Int, val starred: Int)
