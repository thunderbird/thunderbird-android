package net.thunderbird.feature.mail.message.list.internal.ui.state.machine

import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentMap
import net.thunderbird.core.common.state.builder.StateMachineBuilder
import net.thunderbird.feature.mail.message.list.ui.event.FolderEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

/**
 * Sets up global state transitions that can occur from any state.
 *
 * This includes transitions for events that are not specific to a single state,
 * such as updating user preferences. By defining these transitions on the parent
 * [MessageListState], we avoid duplicating the logic in every single sub-state.
 */
internal fun StateMachineBuilder<MessageListState, MessageListEvent>.globalState() {
    state<MessageListState> {
        transition<MessageListEvent.UpdatePreferences> { state, event ->
            state.withPreferences { event.preferences }
        }

        transition<MessageListEvent.ChangeSortCriteria> { state, (accountId, sortCriteria) ->
            val newSortCriteriaPerAccount = state.metadata.sortCriteriaPerAccount + (accountId to sortCriteria)
            state.withMetadata { copy(sortCriteriaPerAccount = newSortCriteriaPerAccount.toPersistentMap()) }
        }

        transition<MessageListEvent.SwipeActionsLoaded> { state, (swipeActions) ->
            state.withMetadata { copy(swipeActions = swipeActions.toImmutableMap()) }
        }

        transition<FolderEvent.FolderLoaded> { state, (folder) ->
            state.withMetadata { copy(folder = folder) }
        }
    }
}
