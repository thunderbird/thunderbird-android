package net.thunderbird.feature.mail.message.list.ui.state

import kotlinx.collections.immutable.ImmutableMap
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.feature.account.AccountId

/**
 * Represents metadata associated with the message list.
 *
 * This class holds information that describes the state of the message list but isn't part of the
 * list data itself, such as loading states or whether more messages can be loaded.
 *
 * @property folder The current folder being displayed. `null` if no folder is selected (e.g. Unified Inbox).
 * @property swipeActions The swipe actions configured for the message list.
 * @property selectedSortTypes The currently selected sorting order for the messages.
 * @property activeMessage The message that is currently being viewed in a split-screen or tablet layout.
 * @property isActive `true` if the message list is the currently active screen; `false` otherwise.
 */
data class MessageListMetadata(
    val folder: Folder?,
    val swipeActions: ImmutableMap<AccountId, SwipeActions>,
    val selectedSortTypes: ImmutableMap<AccountId?, SortType>,
    val activeMessage: MessageItemUi?,
    val isActive: Boolean,
)
