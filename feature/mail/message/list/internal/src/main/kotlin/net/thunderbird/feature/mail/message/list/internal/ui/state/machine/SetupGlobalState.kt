package net.thunderbird.feature.mail.message.list.internal.ui.state.machine

import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentMap
import net.thunderbird.core.common.state.builder.StateMachineBuilder
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

/**
 * Sets up global state transitions that can occur from any state.
 *
 * This includes transitions for events that are not specific to a single state,
 * such as updating user preferences. By defining these transitions on the parent
 * [MessageListState], we avoid duplicating the logic in every single sub-state.
 */
@Suppress("CyclomaticComplexMethod")
internal fun StateMachineBuilder<MessageListState, MessageListEvent>.globalState() {
    state<MessageListState> {
        transition<MessageListEvent.UpdatePreferences> { state, event ->
            when (state) {
                is MessageListState.LoadedMessages -> state.copy(preferences = event.preferences)
                is MessageListState.LoadingMessages -> state.copy(preferences = event.preferences)
                is MessageListState.SearchingMessages -> state.copy(preferences = event.preferences)
                is MessageListState.SelectingMessages -> state.copy(preferences = event.preferences)
                is MessageListState.WarmingUp -> state.copy(preferences = event.preferences)
            }
        }

        transition<MessageListEvent.ChangeSortType> { state, (accountId, sortType) ->
            val newSelectedSortTypes = state.selectedSortTypes.toMutableMap().apply {
                this[accountId] = sortType
            }.toPersistentMap()
            when (state) {
                is MessageListState.LoadedMessages -> state.copy(selectedSortTypes = newSelectedSortTypes)
                is MessageListState.LoadingMessages -> state.copy(selectedSortTypes = newSelectedSortTypes)
                is MessageListState.SearchingMessages -> state.copy(selectedSortTypes = newSelectedSortTypes)
                is MessageListState.SelectingMessages -> state.copy(selectedSortTypes = newSelectedSortTypes)
                is MessageListState.WarmingUp -> state.copy(selectedSortTypes = newSelectedSortTypes)
            }
        }

        transition<MessageListEvent.SwipeActionsLoaded> { state, (swipeActions) ->
            when (state) {
                is MessageListState.LoadedMessages -> state.copy(swipeActions = swipeActions.toImmutableMap())
                is MessageListState.LoadingMessages -> state.copy(swipeActions = swipeActions.toImmutableMap())
                is MessageListState.SearchingMessages -> state.copy(swipeActions = swipeActions.toImmutableMap())
                is MessageListState.SelectingMessages -> state.copy(swipeActions = swipeActions.toImmutableMap())
                is MessageListState.WarmingUp -> state.copy(swipeActions = swipeActions.toImmutableMap())
            }
        }
    }
}
