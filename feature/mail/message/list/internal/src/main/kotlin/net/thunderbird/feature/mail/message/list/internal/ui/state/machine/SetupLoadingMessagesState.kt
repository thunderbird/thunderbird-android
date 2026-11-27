package net.thunderbird.feature.mail.message.list.internal.ui.state.machine

import kotlinx.collections.immutable.toPersistentList
import net.thunderbird.core.common.state.builder.StateMachineBuilder
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
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
 * - On [MessageItemEvent.SetMessageActive]: Updates the active message while the messages are loading, which
 *   re-triggers the message list loading so the list honors the new active message.
 */
internal fun StateMachineBuilder<MessageListState, MessageListEvent>.loadingMessagesState(
    dispatch: (MessageListEvent) -> Unit,
) {
    state<MessageListState.LoadingMessages> {
        transition<MessageListEvent.UpdateLoadingProgress> { state, event ->
            if (event.progress == 1f) {
                dispatch(MessageListEvent.MessagesLoaded(event.messages))
            }
            state.copy(progress = event.progress, messages = event.messages.toPersistentList())
        }
        transition<MessageListEvent.MessagesLoaded>(
            guard = { state, _ -> state.progress == 1f },
        ) { state, event ->
            MessageListState.LoadedMessages(
                metadata = state.metadata,
                preferences = state.preferences,
                messages = event.messages.toPersistentList(),
            )
        }
        transition<MessageItemEvent.SetMessageActive> { state, event ->
            state
                .mapMessages { message ->
                    message.copy(active = message.id == event.message?.id)
                }
                .withMetadata { copy(activeMessage = event.message) }
        }
    }
}
