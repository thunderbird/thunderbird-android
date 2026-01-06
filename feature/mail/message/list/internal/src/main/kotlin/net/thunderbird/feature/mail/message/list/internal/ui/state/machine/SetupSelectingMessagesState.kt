package net.thunderbird.feature.mail.message.list.internal.ui.state.machine

import kotlinx.collections.immutable.toPersistentList
import net.thunderbird.core.common.state.builder.StateMachineBuilder
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
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
internal fun StateMachineBuilder<MessageListState, MessageListEvent>.selectingMessagesState() {
    state<MessageListState.SelectingMessages> {
        transition<MessageItemEvent.ToggleSelectMessages> { state, event ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message in event.messages) message.copy(selected = !message.selected) else message
                }.toPersistentList(),
            )
        }
        transition<MessageListEvent.ExitSelectionMode> { state, _ ->
            MessageListState.LoadedMessages(
                metadata = state.metadata,
                preferences = state.preferences,
                messages = state.messages.map { message -> message.copy(selected = false) }.toPersistentList(),
            )
        }
    }
}
