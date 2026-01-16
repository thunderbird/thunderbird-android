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
 * @param TCurrentState The specific type of the state being configured.
 * @param TState The base type for all states in the state machine.
 * @param TEvent The base type for all events that can trigger transitions.
 */
interface StateWithoutTransactionsBuilder<TCurrentState : TState, TState : Any, TEvent : Any> {
    /**
     * The [KClass] representing the specific state being configured by this builder.
     * This is used to identify the state within the state machine.
     */
    val stateClass: KClass<out TCurrentState>

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
     * before any subsequent events are processed. It is useful for performing setup tasks,
     * logging, or triggering side effects specific to this state.
     *
     * The provided [block] is a lambda with a receiver of the *old state* ([TState]`?`) and
     * two parameters: the `event` that caused the transition and the `newState` ([TCurrentState]).
     *
     * - The receiver (`this`) is the state *before* the transition. It is `null` if the
     *   current state is the initial state of the state machine.
     * - The `event` parameter is the event that triggered the transition into this state. It's
     *   `null` if this is the initial state.
     * - The `newState` parameter is the instance of the state being entered.
     *
     * Example:
     * ```
     * state<MyState.Loading> {
     *     // `this` is the old state, `event` is the trigger, `loadingState` is the new state instance.
     *     onEnter { event, loadingState ->
     *         println("Entered Loading state from ${this?.javaClass?.simpleName} due to event: $event")
     *         // e.g., show a progress spinner using data from loadingState
     *     }
     * }
     * ```
     *
     * @param block A lambda function that will be executed upon entering the state.
     *              It has the old state (`TState?`) as its receiver and receives the triggering
     *              event (`TEvent?`) and the new state instance (`TCurrentState`) as parameters.
     */
    fun onEnter(block: TState?.(event: TEvent?, newState: TCurrentState) -> Unit)

    /**
     * Defines an action to be executed when the state machine exits this state.
     *
     * This action is invoked just before the state machine transitions to a new state. It is useful
     * for performing cleanup tasks, logging, or other side effects associated with leaving a state.
     *
     * The provided [block] is a lambda with the *current state* ([TCurrentState]) as its receiver
     * and two parameters: the `event` that triggered.
     *
     * - The receiver (`this`) is the state instance being exited.
     * - The `event` parameter is the event that triggered the transition out of this state.
     *
     * Note: If this state is marked as a final state (`isFinalState = true`), the `onExit`
     * block will be called immediately after the `onEnter` block, as the machine's lifecycle ends.
     * In this specific case, the `event` parameter will always be `null`.
     *
     * Example:
     * ```
     * state<MyState.Active> {
     *     // `this` is the Active state instance, `event` is the trigger, `newState` is the next state.
     *     onExit { event ->
     *         println("Exiting ${this::class.simpleName} state for new state due to event: $event")
     *         // e.g., stop a timer or hide a UI component
     *     }
     *     // ... transitions
     * }
     * ```
     *
     * @param block A lambda function that will be executed upon exiting the state.
     *              It has the current state ([TCurrentState]) as its receiver and receives the
     */
    fun onExit(block: TCurrentState.(event: TEvent?) -> Unit)
}

/**
 * Base class for state machine state builders.
 *
 * This builder is used to configure transitions and actions for a specific state in the state machine.
 *
 * @param TCurrentState The specific type of the state being configured.
 * @param TState The base type for all states in the state machine.
 * @param TEvent The base type for all events that can trigger transitions.
 */
abstract class BaseStateBuilder<TCurrentState : TState, TState : Any, TEvent : Any> :
    StateWithoutTransactionsBuilder<TCurrentState, TState, TEvent> {

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
internal class InternalStateBuilder<TCurrentState : TState, TState : Any, TEvent : Any>(
    override val stateClass: KClass<out TCurrentState>,
) : BaseStateBuilder<TCurrentState, TState, TEvent>() {
    override var isFinalState = false
    private val transitions = mutableMapOf<TransactionKey<out TState, out TEvent>, Transition<TState, TEvent>>()
    private var onEnter: (TState?.(TEvent?, TCurrentState) -> Unit)? = null
    private var onExit: (TCurrentState.(TEvent?) -> Unit)? = null

    override fun onEnter(block: TState?.(event: TEvent?, newState: TCurrentState) -> Unit) {
        onEnter = block
    }

    override fun onExit(block: TCurrentState.(TEvent?) -> Unit) {
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
    fun build(): StateRegistry<TCurrentState, TState, TEvent> {
        return StateRegistry(
            stateClass = stateClass,
            isFinalState = isFinalState,
            listeners = StateListeners(onEnter = onEnter, onExit = onExit),
            transitions = transitions,
        )
    }
}
