package net.thunderbird.feature.navigation.drawer.dropdown.data

import app.k9mail.legacy.message.controller.MessageCounts
import app.k9mail.legacy.message.controller.MessageCountsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.SearchAccount

internal class FakeMessageCountsProvider(
    private val messageCounts: MessageCounts,
) : MessageCountsProvider {
    var recordedSearch: LocalMessageSearch =
        LocalMessageSearch()

    override fun getMessageCounts(account: LegacyAccountDto): MessageCounts {
        TODO("Not yet implemented")
    }

    override fun getMessageCounts(searchAccount: SearchAccount): MessageCounts {
        TODO("Not yet implemented")
    }

    override fun getMessageCounts(search: LocalMessageSearch): MessageCounts {
        TODO("Not yet implemented")
    }

    override fun getMessageCountsFlow(search: LocalMessageSearch): Flow<MessageCounts> {
        recordedSearch = search
        return flowOf(messageCounts)
    }

    override fun getUnreadMessageCount(account: LegacyAccountDto, folderId: Long): Int {
        TODO("Not yet implemented")
    }
}
