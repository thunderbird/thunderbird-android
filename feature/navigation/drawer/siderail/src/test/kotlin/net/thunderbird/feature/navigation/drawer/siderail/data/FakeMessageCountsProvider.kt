package net.thunderbird.feature.navigation.drawer.siderail.data

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
    var recordedSearch: LocalMessageSearch = LocalMessageSearch()

    override fun getMessageCounts(account: LegacyAccountDto): MessageCounts {
        return messageCounts
    }

    override fun getMessageCounts(searchAccount: SearchAccount): MessageCounts {
        return messageCounts
    }

    override fun getMessageCounts(search: LocalMessageSearch): MessageCounts {
        return messageCounts
    }

    override fun getMessageCountsFlow(search: LocalMessageSearch): Flow<MessageCounts> {
        recordedSearch = search
        return flowOf(messageCounts)
    }

    override fun getUnreadMessageCount(account: LegacyAccountDto, folderId: Long): Int {
        return messageCounts.unread
    }
}
