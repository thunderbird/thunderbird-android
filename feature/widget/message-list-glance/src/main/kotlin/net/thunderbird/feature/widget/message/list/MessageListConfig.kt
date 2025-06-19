package net.thunderbird.feature.widget.message.list

import net.thunderbird.core.android.account.SortType
import net.thunderbird.feature.search.LocalMessageSearch

internal data class MessageListConfig(
    val search: LocalMessageSearch,
    val showingThreadedList: Boolean,
    val sortType: SortType,
    val sortAscending: Boolean,
    val sortDateAscending: Boolean,
)
