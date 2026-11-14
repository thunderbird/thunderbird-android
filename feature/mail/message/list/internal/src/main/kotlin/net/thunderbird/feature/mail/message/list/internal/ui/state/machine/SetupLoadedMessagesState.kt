package net.thunderbird.feature.mail.message.list.internal.ui.state.machine

import kotlinx.collections.immutable.toPersistentList
import net.thunderbird.core.common.state.builder.StateMachineBuilder
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListSearchEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.mail.message.list.ui.state.withState

/**
 * Defines the state transitions for the [MessageListState.LoadedMessages] state.
 *
 * This state represents the default view where the message list has been successfully
 * loaded and is displayed to the user.
 * From here, the user can transition into selection mode or search mode.
 *
 * Transitions:
 * - On [MessageItemEvent.ToggleSelectMessages]: Moves to [MessageListState.SelectingMessages],
 *   toggling the selected state of the specified messages.
 * - On [MessageListEvent.EnterSelectionMode]: Moves to [MessageListState.SelectingMessages]
 *   without changing any message's selected state.
 * - On [MessageListSearchEvent.EnterSearchMode]: Moves to [MessageListState.SearchingMessages]
 *   with an empty search query.
 */
internal fun StateMachineBuilder<MessageListState, MessageListEvent>.loadedMessagesState() {
    state<MessageListState.LoadedMessages> {
        transition<MessageItemEvent.ToggleSelectMessages> { state, event ->
            MessageListState.SelectingMessages(
                metadata = state.metadata,
                preferences = state.preferences,
                messages = state.messages.map { message ->
                    if (message in event.messages) message.copy(selected = !message.selected) else message
                }.toPersistentList(),
            )
        }
        transition<MessageListEvent.EnterSelectionMode> { state, _ ->
            MessageListState.SelectingMessages(
                metadata = state.metadata,
                preferences = state.preferences,
                messages = state.messages,
            )
        }
        transition<MessageListSearchEvent.EnterSearchMode> { state, _ ->
            MessageListState.SearchingMessages(
                searchQuery = "",
                isServerSearch = false,
                metadata = state.metadata,
                preferences = state.preferences,
                messages = state.messages,
            )
        }
        transition<MessageItemEvent.SetMessageActive> { state, event ->
            state
                .mapMessages { message ->
                    // Only a couple of rows change; keep the others as they are instead of copying thousands.
                    val isActive = message.id == event.message?.id
                    if (message.active == isActive) message else message.copy(active = isActive)
                }
                .withMetadata { copy(activeMessage = event.message) }
        }
        transition<MessageItemEvent.OnMessageClick> { state, event ->
            state
                .mapMessages { message ->
                    val isCurrent = message.id == event.message.id
                    val newState = if (isCurrent && message.state != MessageItemUi.State.Read) {
                        MessageItemUi.State.Read
                    } else {
                        message.state
                    }
                    // Only a couple of rows change; keep the others as they are instead of copying thousands.
                    if (message.active == isCurrent && message.state == newState) {
                        message
                    } else {
                        message.copy(active = isCurrent).withState(newState)
                    }
                }
                .withMetadata { copy(activeMessage = event.message) }
        }
    }
}
