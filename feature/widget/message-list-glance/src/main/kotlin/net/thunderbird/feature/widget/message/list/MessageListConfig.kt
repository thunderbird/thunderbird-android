package net.thunderbird.feature.widget.message.list

import net.thunderbird.core.android.account.SortType
import net.thunderbird.feature.search.LocalSearch

internal data class MessageListConfig(
    val search: LocalSearch,
    val showingThreadedList: Boolean,
    val sortType: SortType,
    val sortAscending: Boolean,
    val sortDateAscending: Boolean,
)
