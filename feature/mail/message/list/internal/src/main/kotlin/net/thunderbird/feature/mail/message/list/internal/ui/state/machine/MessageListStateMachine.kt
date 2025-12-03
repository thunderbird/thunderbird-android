package net.thunderbird.feature.mail.message.list.internal.ui.state.machine

import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.common.state.StateMachine
import net.thunderbird.core.common.state.builder.stateMachine
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.StateSideEffectHandler
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

private const val TAG = "MessageListStateMachine"

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
 * @param logger For logging state transitions and events.
 * @param stateSideEffectHandlersFactories A list of factories to create the side effect handlers.
 * @param dispatch A function to send new events back into the state machine, allowing for event-driven side effects.
 * @param stateMachine The underlying state machine implementation, configured with all possible states and transitions.
 */
class MessageListStateMachine(
    private val scope: CoroutineScope,
    private val logger: Logger,
    private val stateSideEffectHandlersFactories: List<StateSideEffectHandler.Factory>,
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
    private val sideEffectHandlers = stateSideEffectHandlersFactories.map { it.create(scope, dispatch) }

    /**
     * Processes an incoming [MessageListEvent] to transition the state machine and trigger side effects.
     *
     * This function takes an event, feeds it to the underlying state machine to compute the next state.
     * If a state transition occurs (i.e., the new state is different from the current one), it logs the change
     * and then delegates to the appropriate [StateSideEffectHandler]s.
     *
     * Only side effect handlers that [StateSideEffectHandler.accept] the given event and the new state
     * will have their [StateSideEffectHandler.handle] method called.
     *
     * @param event The [MessageListEvent] to be processed.
     */
    suspend fun onEvent(event: MessageListEvent) {
        val currentState = stateMachine.currentStateSnapshot
        val newState = stateMachine.process(event)
        if (newState != currentState) {
            logger.verbose(TAG) { "event(${event::class.simpleName}): state update." }
            sideEffectHandlers
                .filter { it.accept(event, newState) }
                .forEach { it.handle(event, currentState, newState) }
        }
    }

    class Factory(
        private val logger: Logger,
        private val stateSideEffectHandlersFactories: List<StateSideEffectHandler.Factory>,
    ) {
        fun create(scope: CoroutineScope, dispatch: (MessageListEvent) -> Unit): MessageListStateMachine {
            val stateMachine = MessageListStateMachine(
                scope = scope,
                logger = logger,
                stateSideEffectHandlersFactories = stateSideEffectHandlersFactories,
                dispatch = dispatch,
            )

            return stateMachine
        }
    }
}
