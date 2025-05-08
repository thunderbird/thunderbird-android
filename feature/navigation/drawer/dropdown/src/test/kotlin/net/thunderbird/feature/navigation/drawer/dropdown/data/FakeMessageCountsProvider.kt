package net.thunderbird.feature.navigation.drawer.dropdown.data

import app.k9mail.legacy.message.controller.MessageCounts
import app.k9mail.legacy.message.controller.MessageCountsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.search.LocalSearch
import net.thunderbird.feature.search.SearchAccount

internal class FakeMessageCountsProvider(
    private val messageCounts: MessageCounts,
) : MessageCountsProvider {
    var recordedSearch: LocalSearch =
        LocalSearch()

    override fun getMessageCounts(account: LegacyAccount): MessageCounts {
        TODO("Not yet implemented")
    }

    override fun getMessageCounts(searchAccount: SearchAccount): MessageCounts {
        TODO("Not yet implemented")
    }

    override fun getMessageCounts(search: LocalSearch): MessageCounts {
        TODO("Not yet implemented")
    }

    override fun getMessageCountsFlow(search: LocalSearch): Flow<MessageCounts> {
        recordedSearch = search
        return flowOf(messageCounts)
    }

    override fun getUnreadMessageCount(account: LegacyAccount, folderId: Long): Int {
        TODO("Not yet implemented")
    }
}
