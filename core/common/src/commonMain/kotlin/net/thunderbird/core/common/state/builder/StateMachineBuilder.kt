package net.thunderbird.core.common.state.builder

import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.common.state.DefaultStateMachine
import net.thunderbird.core.common.state.DefaultStateMachine.StateRegistry
import net.thunderbird.core.common.state.DefaultStateMachine.Transition
import net.thunderbird.core.common.state.StateMachine
import net.thunderbird.core.common.state.TransactionKey

/**
 * A DSL builder for creating [StateMachine] instances.
 * This builder provides a declarative way to define states and their transitions.
 *
 * Use the [stateMachine] function to start building a new state machine.
 *
 * @param TState The base sealed class/interface for all states in the machine.
 * @param TEvent The base sealed class/interface for all events that can trigger transitions.
 *
 * @see StateBuilder
 * @see stateMachine
 */
@StateMachineBuilderDsl
class StateMachineBuilder<TState : Any, TEvent : Any> internal constructor(
    private val scope: CoroutineScope,
) {
    private var initialState: TState? = null
    private val stateRegistrar = mutableMapOf<KClass<out TState>, StateRegistry<TState, TState, TEvent>>()

    /**
     * Defines the initial state of the state machine.
     *
     * Each state machine must have exactly one initial state. This state is also
     * implicitly registered, so there is no need to call `state()` for it separately.
     *
     * @param TCurrentState The specific type of the initial state.
     * @param state The instance of the initial state object. This object's class is used
     *              to identify the state.
     * @param init A lambda with a [StateBuilder] receiver to configure the transitions
     *             and actions (like `onEnter` or `onExit`) for this state.
     */
    fun <TCurrentState : TState> initialState(
        state: TCurrentState,
        init: StateBuilder<TCurrentState, TState, TEvent>.() -> Unit,
    ) = state(state::class, init).also {
        initialState = state
    }

    /**
     * Defines a final state for the state machine.
     *
     * A final state is a terminal state from which no transitions are possible. When the state machine
     * enters a final state, it stops processing any further events. A state machine can have multiple
     * final states.
     *
     * @param TState The base type for all states in the state machine.
     * @param finalStateClass The [KClass] of the state to be marked as final.
     * @param init An optional lambda with a [StateWithoutTransactionsBuilder] receiver to configure actions
     *             for this state, such as `onEnter` or `onExit`.
     */
    fun <TFinalState : TState> finalState(
        finalStateClass: KClass<out TFinalState>,
        init: StateWithoutTransactionsBuilder<TFinalState, TState, TEvent>.() -> Unit = {},
    ) = state(stateClass = finalStateClass) {
        isFinalState = true
        init()
    }

    /**
     * Defines a final state for the state machine.
     *
     * A final state is a terminal state from which no transitions are possible. When the state machine
     * enters a final state, it stops processing any further events. A state machine can have multiple
     * final states.
     *
     * @param TState The base type for all states in the state machine.
     * @param TFinalState The type of the state to be marked as final.
     * @param init An optional lambda with a [StateWithoutTransactionsBuilder] receiver to configure actions
     *             for this state, such as `onEnter` or `onExit`.
     */
    inline fun <reified TFinalState : TState> finalState(
        noinline init: StateWithoutTransactionsBuilder<TFinalState, TState, TEvent>.() -> Unit = {},
    ) = finalState(finalStateClass = TFinalState::class, init = init)

    /**
     * Defines a state and its possible transitions within the state machine.
     * This function registers a new state based on its class, allowing for the configuration
     * of its transitions and actions (e.g., `onEnter`, `onExit`).
     *
     * It is an error to register the same state class more than once. If a state has already
     * been defined as the `initialState`, it cannot be registered again using this function.
     *
     * @param TCurrentState The specific type of the state being defined, which must be a subtype of [TState].
     * @param stateClass The [KClass] of the state to be defined.
     * @param init A lambda with a [StateBuilder] receiver to configure the transitions and actions
     *             for this state.
     * @throws IllegalStateException if the state class is already registered or if it was already
     *                               defined as the initial state.
     */
    fun <TCurrentState : TState> state(
        stateClass: KClass<out TCurrentState>,
        init: StateBuilder<TCurrentState, TState, TEvent>.() -> Unit,
    ) {
        check(stateClass !in stateRegistrar) { "${stateClass.simpleName} is already registered as a state." }
        initialState?.let {
            check(stateClass != it::class) {
                "${stateClass.simpleName} is already defined as initial state."
            }
        }
        val stateRegistry = InternalStateBuilder<TCurrentState, TState, TEvent>(stateClass).apply(init).build()
        @Suppress("UNCHECKED_CAST")
        stateRegistrar += stateClass to stateRegistry as StateRegistry<TState, TState, TEvent>
    }

    /**
     * Defines a state and its possible transitions within the state machine.
     * This function registers a new state based on its class.
     *
     * It's an error to register the same state class more than once or to register the initial
     * state again using this function.
     *
     * @param TCurrentState The type of the state to be defined.
     * @param init A lambda with a [StateBuilder] receiver to configure the transitions and actions for this state.
     * @throws IllegalStateException if the state is already registered or if it's already defined as the initial state.
     */
    inline fun <reified TCurrentState : TState> state(
        noinline init: StateBuilder<TCurrentState, TState, TEvent>.() -> Unit,
    ) = state(stateClass = TCurrentState::class, init)

    /**
     * Builds and validates the [StateMachine] instance.
     * This function should be called after all states and transitions have been defined.
     *
     * It performs several checks to ensure the state machine is valid:
     * - An initial state must be set.
     * - There must be at least two states defined.
     * - There must be at least one transition defined in the entire machine.
     * - All non-final states must have at least one outgoing transition.
     *
     * @return A fully configured and validated [StateMachine] instance.
     * @throws IllegalStateException if any of the validation checks fail.
     */
    fun build(): StateMachine<TState, TEvent> {
        val initialState = checkNotNull(initialState) { "Initial state is required." }
        check(stateRegistrar.size > 1) { "At least two states must be defined." }

        val transitions = stateRegistrar.values.fold(
            initial = emptyMap<TransactionKey<out TState, out TEvent>, Transition<TState, TEvent>>(),
        ) { acc, registry ->
            acc + registry.transitions
        }

        check(transitions.isNotEmpty()) { "At least one transition must be defined." }
        val nonFinalStatesWithoutTransitions = stateRegistrar.filterValues { registry ->
            !registry.isFinalState && registry.transitions.isEmpty()
        }
        check(nonFinalStatesWithoutTransitions.isEmpty()) {
            "Only the final states can have no transitions. States without transaction: [ ${
                nonFinalStatesWithoutTransitions.entries.joinToString(", ") { (stateClass, _) ->
                    stateClass.simpleName ?: stateClass.toString()
                }
            } ]"
        }
        return DefaultStateMachine(
            scope = scope,
            initialState = initialState,
            transitions = transitions,
            stateRegistrar = stateRegistrar,
        )
    }
}
