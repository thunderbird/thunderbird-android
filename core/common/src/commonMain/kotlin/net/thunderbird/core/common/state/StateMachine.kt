package net.thunderbird.core.common.state

import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.thunderbird.core.common.state.StateMachine.Transition
import net.thunderbird.core.common.state.builder.StateMachineDebugger
import net.thunderbird.core.logging.Logger

internal typealias TransactionKey<TState, TEvent> = Pair<KClass<out TState>, KClass<out TEvent>>

/**
 * Defines the core contract for a generic, thread-safe state machine.
 *
 * This interface provides a standardized way to manage state transitions in response to events.
 * It is designed to be asynchronous, using Kotlin Coroutines and [StateFlow] to expose the
 * current state to observers. The state machine guarantees that event processing and state
 * updates are atomic operations.
 *
 * Implementations of this interface allow for defining a graph of states and the transitions
 * between them, which are triggered by specific events.
 *
 * @param TState The base sealed interface for all possible states.
 * @param TEvent The base sealed interface for all possible events.
 */
interface StateMachine<TState : Any, TEvent : Any> {
    val currentState: StateFlow<TState>
    val currentStateSnapshot: TState get() = currentState.value

    /**
     * Represents the history of state transitions in the state machine.
     *
     * This list contains the sequence of transitions that have occurred, in the order
     * they were executed. Each transition encapsulates the logic used to determine
     * if a state change should occur and the function to generate the new state.
     * Observing or analyzing this history can provide insights into the state machine's
     * behavior and processing flow.
     *
     * The history stack is immutable and is updated atomically within the state machine
     * during each valid transition, ensuring thread safety and consistency.
     *
     * @param TState The type representing the states managed by the state machine.
     * @param TEvent The type representing the events processed by the state machine.
     */
    val historyStack: List<StateTransitionRecord<TState, TEvent>>

    /**
     * Processes an incoming event against the current state.
     *
     * This function looks for a defined transition that matches the current state's type and the
     * provided event's type. If a matching transition is found and its `guard` condition passes,
     * the state machine will transition to a new state. The new state is then emitted to observers
     * of the [currentState] flow.
     *
     * If no transition is defined for the state/event pair, or if the `guard` condition
     * returns `false`, the event is ignored, and the state remains unchanged.
     *
     * This operation is thread-safe, ensuring that state transitions are processed atomically.
     *
     * @param event The event to process.
     * @return The new state if a transition occurred, or the current state if no transition was made.
     */
    suspend fun process(event: TEvent): TState

    /**
     * Represents a state transition in a state machine.
     *
     * A transition defines the conditions under which a state machine moves from one state to another
     * based on the current state and an incoming event. Each transition consists of a [guard] function that
     * determines whether the transition is valid and a state creation function that generates the new state.
     *
     * @param TState The type representing the states managed by the state machine.
     * @param TEvent The type representing the events triggering transitions.
     * @property guard A condition function that determines if the transition is valid. The function takes
     *  the current state and the event as input and returns `true` if the transition can occur, or `false`
     *  otherwise.
     * @property createNewState A function responsible for generating the new state after a transition. The
     *  function takes the current state and the event as input and returns the new state.
     */
    data class Transition<TState : Any, in TEvent : Any>(
        val guard: (TState, TEvent) -> Boolean,
        val createNewState: (TState, TEvent) -> TState,
    )

    /**
     * Represents a record of a state transition in a state machine.
     *
     * This data class encapsulates the details of a state transition, including the specific transition
     * object that governs the change, the state before the transition, and the state after the transition.
     *
     * @param TState The type representing the states managed by the state machine.
     * @param TEvent The type representing the events that trigger state transitions.
     * @property transition The transition object that defines the conditions and rules of the state change.
     * @property previousState The state of the state machine before the transition occurred.
     * @property newState The state of the state machine after the transition occurred.
     */
    @OptIn(ExperimentalTime::class)
    data class StateTransitionRecord<TState : Any, TEvent : Any>(
        val transition: Transition<TState, TEvent>,
        val event: TEvent,
        val previousState: TState,
        val newState: TState,
        val timestamp: Instant,
    )
}

internal class DefaultStateMachine<TState : Any, TEvent : Any>(
    private val logger: Logger?,
    private val logTag: String?,
    private val stateMachineDebugger: StateMachineDebugger<TState, TEvent>?,
    scope: CoroutineScope,
    initialState: TState,
    internal val stateRegistrar: Map<KClass<out TState>, StateRegistry<TState, TState, TEvent>>,
    private val transitions: Map<TransactionKey<out TState, out TEvent>, Transition<TState, TEvent>>,
) : StateMachine<TState, TEvent> {
    data class StateRegistry<TCurrentState : TState, TState : Any, TEvent : Any>(
        val stateClass: KClass<out TState>,
        val isFinalState: Boolean,
        val listeners: StateListeners<TCurrentState, TState, TEvent>,
        val transitions: Map<TransactionKey<out TState, out TEvent>, Transition<TState, TEvent>>,
    )

    data class StateListeners<in TCurrentState : TState, TState : Any, in TEvent : Any>(
        val onEnter: (TState?.(event: TEvent?, newState: TCurrentState) -> Unit)?,
        val onExit: (TCurrentState.(event: TEvent?) -> Unit)?,
    )

    private val mutex = Mutex()
    private val _currentState = MutableStateFlow(initialState)
    override val currentState: StateFlow<TState> = _currentState.asStateFlow()
    override val historyStack: List<StateMachine.StateTransitionRecord<TState, TEvent>>
        get() = stateMachineDebugger?.historyStack ?: emptyList()

    init {
        scope.launch {
            // delay onEnter initialization so the viewModels are ready to receive the state
            delay(500.milliseconds)
            verbose("StateMachine initialized with initial state: $initialState")
            stateRegistrar[initialState::class]?.listeners?.onEnter?.invoke(null, null, currentStateSnapshot)
        }
    }

    override suspend fun process(event: TEvent): TState {
        debug("Processing event: $event")
        mutex.withLock {
            val currentState = _currentState.value
            val currentStateRegistry = stateRegistrar[currentState::class]
            if (currentStateRegistry?.isFinalState == true) return currentState

            val key = currentState::class to event::class
            val transition = transitions[key]
                ?: transitions
                    .filterKeys { (stateClass, eventClass) ->
                        stateClass.isInstance(currentState) && eventClass == event::class
                    }
                    .firstNotNullOfOrNull { it.value }
            if (transition == null) {
                verbose("Found no transition for $currentState -> $event")
            }

            return when {
                transition != null && transition.guard(currentState, event) -> {
                    val newState = transition.createNewState(currentState, event)
                    val newStateRegistry = stateRegistrar[newState::class]
                    when {
                        newStateRegistry?.isFinalState == true -> {
                            currentStateRegistry?.listeners?.onExit?.invoke(currentState, event)
                            newStateRegistry.listeners.onEnter?.invoke(currentState, event, newState)
                            newStateRegistry.listeners.onExit?.invoke(newState, null)
                            verbose("Reached final state: ${newState::class.simpleName}")
                        }

                        currentState::class != newState::class -> {
                            debug(
                                "Transitioning from ${currentState::class.simpleName} to ${newState::class.simpleName}",
                            )
                            currentStateRegistry?.listeners?.onExit?.invoke(currentState, event)
                            newStateRegistry?.listeners?.onEnter?.invoke(currentState, event, newState)
                        }
                    }
                    _currentState.update { newState }
                    stateMachineDebugger?.record(
                        transition = transition,
                        event = event,
                        previousState = currentState,
                        newState = newState,
                    )
                    newState.also {
                        debug("Transitioned from ${currentState::class.simpleName} to ${newState::class.simpleName}")
                    }
                }

                else -> {
                    info(
                        "No transition defined for '${currentState::class.simpleName}' with event '$event', " +
                            "or guard failed; Ignore event",
                    )
                    // No transition defined, or guard failed -> Ignore event
                    currentState
                }
            }.also {
                stateMachineDebugger?.dump()
            }
        }
    }

    private fun verbose(message: String) {
        if (stateMachineDebugger != null) {
            logger?.verbose(logTag) { message }
        }
    }

    private fun debug(message: String) {
        if (stateMachineDebugger != null) {
            logger?.debug(logTag) { message }
        }
    }

    private fun info(message: String) {
        logger?.info(logTag) { message }
    }
}
