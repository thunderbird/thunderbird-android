package net.thunderbird.core.common.state.builder

import kotlin.contracts.ExperimentalContracts
import kotlin.reflect.KClass
import net.thunderbird.core.common.state.DefaultStateMachine.StateListeners
import net.thunderbird.core.common.state.DefaultStateMachine.StateRegistry
import net.thunderbird.core.common.state.DefaultStateMachine.Transition
import net.thunderbird.core.common.state.StateMachine
import net.thunderbird.core.common.state.TransactionKey

/**
 * A builder for a state in a [StateMachine] that does not have any transitions defined yet.
 * This is the initial builder returned when defining a new state.
 *
 * @param TEvent The base type for all events that can trigger transitions.
 */
interface StateWithoutTransactionsBuilder<TState : Any, TEvent : Any> {
    /**
     * The [KClass] representing the specific state being configured by this builder.
     * This is used to identify the state within the state machine.
     */
    val stateClass: KClass<out TState>

    /**
     * A flag indicating whether this state is a final state in the state machine.
     *
     * When a state machine enters a final state, it is considered "finished" and will no longer
     * process any new events. Defaults to `false`.
     */
    var isFinalState: Boolean

    /**
     * Defines an action to be executed when the state machine enters this state.
     *
     * This action is triggered immediately after the transition to this state is complete, but
     * before any subsequent events are processed. It's useful for performing setup tasks,
     * logging, or triggering side effects specific to this state.
     *
     * The provided `block` will receive the event that caused the transition into this state.
     * If the state is the initial state of the state machine, the event will be `null`.
     *
     * Example:
     * ```
     * state<MyState.Loading> {
     *     onEnter { event ->
     *         println("Entered Loading state due to event: $event")
     *         // e.g., show a progress spinner
     *     }
     * }
     * ```
     *
     * @param block A lambda function that will be executed upon entering the state. It receives
     *              the event that triggered the transition, or `null` if it's the initial state.
     */
    fun onEnter(block: (TEvent?) -> Unit)

    /**
     * Defines an action to be executed when the state machine exits this state.
     *
     * This block is invoked just before the state machine transitions to a new state.
     * It receives the event that triggered the transition as its parameter.
     * This is useful for performing cleanup or logging when leaving a state.
     *
     * Note: If this state is marked as a final state (`isFinalState = true`),
     * the `onExit` block will be called right after the `onEnter` block.
     *
     * Example:
     * ```
     * state<MyState> {
     *     onExit { event ->
     *         println("Exiting MyState due to event: $event")
     *     }
     *     // ... transitions
     * }
     * ```
     *
     * @param block A lambda function that takes the triggering event of type [TEvent] and performs an action.
     */
    fun onExit(block: (TEvent) -> Unit)
}

/**
 * A builder for defining a specific state within a [StateMachine].
 *
 * This builder provides a DSL for configuring a state, including:
 * - Defining transitions to other states via the [transition] function.
 * - Setting entry and exit actions with [onEnter] and [onExit].
 * - Marking a state as final with [isFinalState].
 *
 * It inherits from [StateWithoutTransactionsBuilder] to provide the base configuration methods.
 *
 * @param TCurrentState The specific type of the state being configured.
 * @param TState The base type for all states in the state machine.
 * @param TEvent The base type for all events that can trigger transitions.
 */
abstract class StateBuilder<TCurrentState : TState, TState : Any, TEvent : Any> :
    StateWithoutTransactionsBuilder<TCurrentState, TEvent> {

    /**
     * Defines a transition from the current state to a new state upon receiving a specific event.
     *
     * This function is used within the state machine DSL to declare how the machine should react
     * to an event when it's in the state being configured. The transition will only occur if the
     * incoming event matches the [targetEvent] and the optional [guard] condition is met.
     *
     * @param targetEvent The [KClass] of the event that triggers this transition.
     * @param guard An optional predicate function that must return `true` for the transition to proceed.
     *              It receives the current state and the incoming event. If not provided, it defaults
     *              to a function that always returns `true`.
     * @param block A function that executes to produce the new state. It receives the current
     *              state and the incoming event, and its return value becomes the new state of the
     *              state machine.
     */
    abstract fun transition(
        currentState: KClass<out TCurrentState>,
        targetEvent: KClass<out TEvent>,
        guard: (currentState: TCurrentState, event: TEvent) -> Boolean = { _, _ -> true },
        block: (currentState: TCurrentState, event: TEvent) -> TState,
    )

    /**
     * Defines a transition from the current state to a new state upon receiving a specific event.
     *
     * This function is used within the state machine DSL to declare how the machine should react
     * to an event when it's in the state being configured. The transition will only occur if the
     * incoming event matches the [TTargetEvent] and the optional [guard] condition is met.
     *
     * @param TTargetEvent The [KClass] of the event that triggers this transition.
     * @param guard An optional predicate function that must return `true` for the transition to proceed.
     *              It receives the current state and the incoming event. If not provided, it defaults
     *              to a function that always returns `true`.
     * @param block A function that executes to produce the new state. It receives the current
     *              state and the incoming event, and its return value becomes the new state of the
     *              state machine.
     */
    @OptIn(ExperimentalContracts::class)
    inline fun <reified TTargetEvent : TEvent> transition(
        noinline guard: (currentState: TCurrentState, event: TTargetEvent) -> Boolean = { _, _ -> true },
        noinline block: (currentState: TCurrentState, event: TTargetEvent) -> TState,
    ) {
        transition(
            currentState = stateClass,
            targetEvent = TTargetEvent::class,
            guard = { currentState, event -> guard(currentState, event as TTargetEvent) },
            block = { currentState, event -> block(currentState, event as TTargetEvent) },
        )
    }
}

@StateMachineBuilderDsl
internal class StateBuilderImpl<TCurrentState : TState, TState : Any, TEvent : Any>(
    override val stateClass: KClass<out TCurrentState>,
) : StateBuilder<TCurrentState, TState, TEvent>() {
    override var isFinalState = false
    private val transitions = mutableMapOf<TransactionKey<out TState, out TEvent>, Transition<TState, TEvent>>()
    private var onEntry: ((TEvent?) -> Unit)? = null
    private var onExit: ((TEvent) -> Unit)? = null

    override fun onEnter(block: (TEvent?) -> Unit) {
        onEntry = block
    }

    override fun onExit(block: (TEvent) -> Unit) {
        onExit = block
    }

    override fun transition(
        currentState: KClass<out TCurrentState>,
        targetEvent: KClass<out TEvent>,
        guard: (currentState: TCurrentState, event: TEvent) -> Boolean,
        block: (currentState: TCurrentState, event: TEvent) -> TState,
    ) {
        val key = stateClass to targetEvent

        // We wrap the specific types in a generic handler to store them safely
        @Suppress("UNCHECKED_CAST")
        val transition = Transition<TState, TEvent>(
            guard = { s, e -> guard(s as TCurrentState, e) },
            createNewState = { s, e -> block(s as TCurrentState, e) },
        )

        transitions[key] = transition
    }

    /**
     * Finalizes the configuration for the current state and constructs the internal representation
     * used by the state machine.
     *
     * This function is called internally by the state machine builder after the DSL block for this
     * state has been executed. It gathers all the defined transitions, `onEnter`/`onExit` actions,
     * and the `isFinalState` flag, and packages them into a [StateRegistry] object.
     *
     * @return A [Pair] where the first element is the [KClass] of the state being built, and the
     *         second element is the [DefaultStateMachine.StateRegistry] containing all the
     *         configuration for that state.
     */
    fun build(): StateRegistry<TState, TEvent> {
        return StateRegistry(
            stateClass = stateClass,
            isFinalState = isFinalState,
            listeners = StateListeners(
                onEntry = onEntry,
                onExit = onExit,
            ),
            transitions = transitions,
        )
    }
}
