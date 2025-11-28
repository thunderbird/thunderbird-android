package net.thunderbird.core.common.state

import kotlin.reflect.KClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal typealias TransactionKey<TState, TEvent> = Pair<KClass<out TState>, KClass<out TEvent>>

/**
 * A generic, thread-safe state machine implementation for managing state transitions in response to events.
 *
 * This class allows defining transitions between different states based on specific events.
 * It uses Kotlin Coroutines and Flow to manage state changes asynchronously and expose the
 * current state to observers. Transitions can be defined using a simple DSL with the [on] function,
 * which can include optional guard conditions to determine if a transition should occur.
 *
 * The state machine is thread-safe, ensuring that event processing and state updates are atomic operations.
 *
 * Example Usage:
 * ```
 * // 1. Define states and events
 * sealed interface MyState : State
 * data object Idle : MyState
 * data object Loading : MyState
 * data class Success(val data: String) : MyState
 *
 * sealed interface MyEvent : Event
 * data object FetchData : MyEvent
 * data class DataLoaded(val data: String) : MyEvent
 *
 * // 2. Create and configure the state machine
 * val stateMachine = StateMachine<MyState, MyEvent>(initialState = Idle).apply {
 *     on<Idle, FetchData> { _, _ -> Loading }
 *     on<Loading, DataLoaded> { _, event -> Success(event.data) }
 * }
 *
 * // 3. Process an event
 * viewModelScope.launch {
 *     stateMachine.process(FetchData)
 *     // currentState is now Loading
 * }
 * ```
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
    initialState: TState,
    internal val stateRegistrar: Map<KClass<out TState>, StateRegistry<TState, TEvent>>,
    private val transitions: Map<TransactionKey<out TState, out TEvent>, Transition<TState, TEvent>>,
) : StateMachine<TState, TEvent> {
    data class StateRegistry<TState : Any, TEvent : Any>(
        val stateClass: KClass<out TState>,
        val isFinalState: Boolean,
        val listeners: StateListeners<TEvent>,
        val transitions: Map<TransactionKey<out TState, out TEvent>, Transition<TState, TEvent>>,
    )

    data class Transition<TState : Any, in TEvent : Any>(
        val guard: (TState, TEvent) -> Boolean,
        val createNewState: (TState, TEvent) -> TState,
    )

    data class StateListeners<in TEvent : Any>(
        val onEntry: ((TEvent?) -> Unit)?,
        val onExit: ((TEvent) -> Unit)?,
    )

    private val mutex = Mutex()
    private val _currentState = MutableStateFlow(initialState)
    override val currentState: StateFlow<TState> = _currentState.asStateFlow()

    init {
        stateRegistrar[initialState::class]?.listeners?.onEntry?.invoke(null)
    }

    override suspend fun process(event: TEvent): TState {
        mutex.withLock {
            val currentStateRegistry = stateRegistrar[_currentState.value::class]
            if (currentStateRegistry?.isFinalState == true) return currentState.value

            val key = _currentState.value::class to event::class
            val transition = transitions[key]

            return when {
                transition != null && transition.guard(_currentState.value, event) -> {
                    transition.createNewState(_currentState.value, event).also { newState ->
                        _currentState.update { currentState ->
                            val newStateRegistry = stateRegistrar[newState::class]
                            when {
                                newStateRegistry?.isFinalState == true -> {
                                    currentStateRegistry?.listeners?.onExit?.invoke(event)
                                    newStateRegistry.listeners.onEntry?.invoke(event)
                                    newStateRegistry.listeners.onExit?.invoke(event)
                                }

                                currentState::class != newState::class -> {
                                    currentStateRegistry?.listeners?.onExit?.invoke(event)
                                    newStateRegistry?.listeners?.onEntry?.invoke(event)
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
