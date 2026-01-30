package net.thunderbird.feature.mail.message.list.internal.ui.state.machine

import net.thunderbird.core.common.state.builder.StateMachineBuilder
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListSearchEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

/**
 * Defines the state transitions for the [MessageListState.SearchingMessages] state.
 *
 * This state handles the logic for when the user is actively searching for messages. It manages
 * transitions for updating the search query, switching to a remote (server-side) search, and
 * exiting the search mode to return to the normal message list view.
 *
 * - On [MessageListSearchEvent.UpdateSearchQuery]: Updates the current search query and resets to local search.
 * - On [MessageListSearchEvent.SearchRemotely]: Sets the search to be performed on the server.
 * - On [MessageListSearchEvent.ExitSearchMode]: Transitions back to the [MessageListState.LoadedMessages] state,
 *   effectively ending the search.
 */
internal fun StateMachineBuilder<MessageListState, MessageListEvent>.searchingMessagesState() {
    state<MessageListState.SearchingMessages> {
        transition<MessageListSearchEvent.UpdateSearchQuery> { state, event ->
            state.copy(searchQuery = event.query)
        }

        transition<MessageListSearchEvent.SearchRemotely> { state, _ ->
            state.copy(
                searchQuery = state.searchQuery,
                isServerSearch = true,
            )
        }

        transition<MessageListSearchEvent.ExitSearchMode> { state, _ ->
            MessageListState.LoadedMessages(
                metadata = state.metadata,
                preferences = state.preferences,
                messages = state.messages,
            )
        }
    }
}
