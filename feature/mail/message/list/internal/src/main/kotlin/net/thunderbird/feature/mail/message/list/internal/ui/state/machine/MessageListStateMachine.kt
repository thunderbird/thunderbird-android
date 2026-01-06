package net.thunderbird.feature.mail.message.list.internal.ui.state.machine

import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.common.state.StateMachine
import net.thunderbird.core.common.state.builder.stateMachine
import net.thunderbird.core.common.state.sideeffect.StateSideEffectHandler
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

/**
 * Manages the state transitions for the message list UI.
 *
 * This class orchestrates the overall state of the message list screen by processing incoming [MessageListEvent]s
 * and transitioning between different [MessageListState]s. It encapsulates the core logic for how the UI should
 * react to user actions and data loading events.
 *
 * The state machine is defined using a DSL that configures transitions for various states like loading,
 * displaying messages, selection mode, and search mode.
 *
 * Upon a state change, it delegates to a list of [StateSideEffectHandler]s, which are responsible for
 * executing side effects such as fetching data from a repository, navigating, or showing toasts. This separation
 * keeps the state management logic clean and focused.
 *
 * @param dispatch A function to send new events back into the state machine, allowing for event-driven side effects.
 * @param stateMachine The underlying state machine implementation, configured with all possible states and transitions.
 */
class MessageListStateMachine(
    private val scope: CoroutineScope,
    private val dispatch: (MessageListEvent) -> Unit,
    private val stateMachine: StateMachine<MessageListState, MessageListEvent> = stateMachine(scope) {
        warmingUpInitialState(initialState = MessageListState.WarmingUp(), dispatch)
        globalState()
        loadingMessagesState()
        loadedMessagesState()
        selectingMessagesState()
        searchingMessagesState()
    },
) : StateMachine<MessageListState, MessageListEvent> by stateMachine {
    class Factory {
        fun create(
            scope: CoroutineScope,
            dispatch: (MessageListEvent) -> Unit,
        ): MessageListStateMachine = MessageListStateMachine(
            scope = scope,
            dispatch = dispatch,
        )
    }
}
