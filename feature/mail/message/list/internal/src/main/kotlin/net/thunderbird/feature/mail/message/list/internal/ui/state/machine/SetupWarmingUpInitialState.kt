package net.thunderbird.feature.mail.message.list.internal.ui.state.machine

import kotlinx.collections.immutable.toImmutableMap
import net.thunderbird.core.common.state.builder.StateMachineBuilder
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

/**
 * Defines the behavior for the [MessageListState.WarmingUp] state.
 *
 * This state is responsible for loading initial configurations, such as user preferences,
 * swipe actions, and sort types, before transitioning to the message loading state.
 *
 * - On entering this state, it dispatches [MessageListEvent.LoadConfigurations] to trigger the loading process.
 * - It handles updates for preferences, swipe actions, and sort types as they become available.
 * - Once all required configurations are loaded ([MessageListState.WarmingUp.isReady] is `true`),
 *   it transitions to the [MessageListState.LoadingMessages] state upon receiving the
 *   [MessageListEvent.AllConfigsReady] event.
 *
 * @param initialState The initial [MessageListState.WarmingUp] instance.
 * @param dispatch A function to send events to the state machine.
 */
internal fun StateMachineBuilder<MessageListState, MessageListEvent>.warmingUpInitialState(
    initialState: MessageListState.WarmingUp,
    dispatch: (MessageListEvent) -> Unit = {},
) {
    initialState(state = initialState) {
        onEnter { _, _ ->
            dispatch(MessageListEvent.LoadConfigurations)
        }
        transition<MessageListEvent.LoadConfigurations> { state, _ -> state.withMetadata { copy(isActive = true) } }
        transition<MessageListEvent.UpdatePreferences> { state, event ->
            state.copy(preferences = event.preferences)
        }
        transition<MessageListEvent.SortTypesLoaded> { state, event ->
            state.withMetadata { copy(selectedSortTypes = event.sortTypes.toImmutableMap()) }
        }
        transition<MessageListEvent.AllConfigsReady>(
            guard = { state, _ -> state.isReady },
        ) { state, _ ->
            MessageListState.LoadingMessages(
                progress = 0f,
                metadata = state.metadata,
                preferences = requireNotNull(state.preferences),
            )
        }
    }
}
