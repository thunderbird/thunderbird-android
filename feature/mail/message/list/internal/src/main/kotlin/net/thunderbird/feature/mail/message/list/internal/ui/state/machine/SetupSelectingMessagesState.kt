package net.thunderbird.feature.mail.message.list.internal.ui.state.machine

import kotlinx.collections.immutable.toPersistentList
import net.thunderbird.core.common.state.builder.StateMachineBuilder
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

/**
 * Defines the state transitions for when the user is actively selecting messages.
 *
 * This state is entered when the user long-presses a message, initiating selection mode.
 *
 * It handles the following events:
 *  - [MessageItemEvent.ToggleSelectMessages]: Toggles the selection status of one or more messages.
 *  - [MessageListEvent.ExitSelectionMode]: Exits selection mode, deselecting all messages and returning
 *   to the [MessageListState.LoadedMessages] state.
 */
internal fun StateMachineBuilder<MessageListState, MessageListEvent>.selectingMessagesState(
    dispatch: (MessageListEvent) -> Unit,
    dispatchUiEffect: (MessageListEffect) -> Unit,
) {
    state<MessageListState.SelectingMessages> {
        transition<MessageItemEvent.ToggleSelectMessages> { state, event ->
            toggleSelectMessages(state, event.messages, dispatch, dispatchUiEffect)
        }

        transition<MessageListEvent.ExitSelectionMode> { state, _ ->
            dispatchUiEffect(MessageListEffect.ResetToolbarActionMode)
            MessageListState.LoadedMessages(
                metadata = state.metadata,
                preferences = state.preferences,
                messages = state.messages.map { message -> message.copy(selected = false) }.toPersistentList(),
            )
        }

        transition<MessageItemEvent.OnMessageClick> { state, event ->
            toggleSelectMessages(state, listOf(event.message), dispatch, dispatchUiEffect)
        }
    }
}

private fun toggleSelectMessages(
    state: MessageListState.SelectingMessages,
    messages: List<MessageItemUi>,
    dispatch: (MessageListEvent) -> Unit,
    dispatchUiEffect: (MessageListEffect) -> Unit,
): MessageListState.SelectingMessages {
    var selectedCount = 0
    val newMessages = state.messages.map { message ->
        if (message in messages) {
            message.copy(selected = !message.selected)
        } else {
            message
        }.also { selectedCount += if (it.selected) 1 else 0 }
    }.toPersistentList()
    return if (selectedCount == 0) {
        dispatch(MessageListEvent.ExitSelectionMode)
        dispatchUiEffect(MessageListEffect.ResetToolbarActionMode)
        state
    } else {
        state.copy(messages = newMessages)
    }
}
