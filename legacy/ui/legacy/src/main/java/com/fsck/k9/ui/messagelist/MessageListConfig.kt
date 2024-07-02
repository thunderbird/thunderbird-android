package com.fsck.k9.ui.messagelist

import com.fsck.k9.Account.SortType
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.search.LocalSearch

data class MessageListConfig(
    val search: LocalSearch,
    val showingThreadedList: Boolean,
    val sortType: SortType,
    val sortAscending: Boolean,
    val sortDateAscending: Boolean,
    val activeMessage: MessageReference?,
    val sortOverrides: Map<MessageReference, MessageSortOverride>,
)

data class MessageSortOverride(
    val isRead: Boolean,
    val isStarred: Boolean,
)
