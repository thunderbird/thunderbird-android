package app.k9mail.feature.widget.message.list

import com.fsck.k9.Account.SortType
import com.fsck.k9.search.LocalSearch

internal data class MessageListConfig(
    val search: LocalSearch,
    val showingThreadedList: Boolean,
    val sortType: SortType,
    val sortAscending: Boolean,
    val sortDateAscending: Boolean,
)
