package net.thunderbird.feature.mail.message.list.internal.ui.state.machine

import kotlinx.collections.immutable.toPersistentList
import net.thunderbird.core.common.state.builder.StateMachineBuilder
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

/**
 * Defines the behavior of the state machine when it is in the [MessageListState.LoadingMessages] state.
 *
 * This state handles the following transitions:
 * - On [MessageListEvent.UpdateLoadingProgress]: Updates the loading progress indicator.
 * - On [MessageListEvent.MessagesLoaded]: Transitions to the [MessageListState.LoadedMessages] state, but only
 *   if the loading progress has reached 100% (`progress == 1f`). This ensures a smooth transition
 *   after the loading animation completes.
 */
internal fun StateMachineBuilder<MessageListState, MessageListEvent>.loadingMessagesState() {
    state<MessageListState.LoadingMessages> {
        transition<MessageListEvent.UpdateLoadingProgress> { state, event ->
            state.copy(progress = event.progress)
        }
        transition<MessageListEvent.MessagesLoaded>(
            guard = { state, _ -> state.progress == 1f },
        ) { state, event ->
            MessageListState.LoadedMessages(
                folder = null,
                messages = event.messages.toPersistentList(),
                preferences = state.preferences,
                swipeActions = state.swipeActions,
                selectedSortTypes = state.selectedSortTypes,
                activeMessage = null,
                isActive = true,
            )
        }
    }
}
