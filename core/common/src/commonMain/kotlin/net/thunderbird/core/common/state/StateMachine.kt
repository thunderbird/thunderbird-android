package net.thunderbird.core.common.state

import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
}

internal class DefaultStateMachine<TState : Any, TEvent : Any>(
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

    data class Transition<TState : Any, in TEvent : Any>(
        val guard: (TState, TEvent) -> Boolean,
        val createNewState: (TState, TEvent) -> TState,
    )

    data class StateListeners<in TCurrentState : TState, TState : Any, in TEvent : Any>(
        val onEnter: (TState?.(event: TEvent?, newState: TCurrentState) -> Unit)?,
        val onExit: (TCurrentState.(event: TEvent?) -> Unit)?,
    )

    private val mutex = Mutex()
    private val _currentState = MutableStateFlow(initialState)
    override val currentState: StateFlow<TState> = _currentState.asStateFlow()

    init {
        scope.launch {
            // delay onEnter initialization so the viewModels are ready to receive the state
            delay(500.milliseconds)
            stateRegistrar[initialState::class]?.listeners?.onEnter?.invoke(null, null, currentStateSnapshot)
        }
    }

    override suspend fun process(event: TEvent): TState {
        mutex.withLock {
            val currentStateRegistry = stateRegistrar[_currentState.value::class]
            if (currentStateRegistry?.isFinalState == true) return currentState.value

            val key = _currentState.value::class to event::class
            val transition = transitions[key]
                ?: transitions
                    .filterKeys { (stateClass, eventClass) ->
                        stateClass.isInstance(_currentState.value) && eventClass == event::class
                    }
                    .firstNotNullOfOrNull { it.value }

            return when {
                transition != null && transition.guard(_currentState.value, event) -> {
                    transition.createNewState(_currentState.value, event).also { newState ->
                        _currentState.update { currentState ->
                            val newStateRegistry = stateRegistrar[newState::class]
                            when {
                                newStateRegistry?.isFinalState == true -> {
                                    currentStateRegistry?.listeners?.onExit?.invoke(currentState, event)
                                    newStateRegistry.listeners.onEnter?.invoke(currentState, event, newState)
                                    newStateRegistry.listeners.onExit?.invoke(newState, null)
                                }

                                currentState::class != newState::class -> {
                                    currentStateRegistry?.listeners?.onExit?.invoke(currentState, event)
                                    newStateRegistry?.listeners?.onEnter?.invoke(currentState, event, newState)
                                }
                            }

                            newState
                        }
                    }
                }

                else -> {
                    // No transition defined or guard failed -> Ignore event
                    _currentState.value
                }
            }
        }
    }
}
