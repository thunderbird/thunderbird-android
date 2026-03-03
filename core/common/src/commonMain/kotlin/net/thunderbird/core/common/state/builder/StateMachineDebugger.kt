package net.thunderbird.core.common.state.builder

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import net.thunderbird.core.common.state.StateMachine
import net.thunderbird.core.common.state.debug.StatePrettyPrinter
import net.thunderbird.core.logging.Logger

/**
 * A debugging utility for tracking and logging state machine transitions.
 *
 * This internal debugger maintains a history of state transitions and provides functionality
 * to record state changes and dump the transition history in a formatted manner. It is designed
 * to help with troubleshooting and monitoring state machine behavior during development and debugging.
 *
 * @param TState The type representing the states in the state machine. Must be a non-nullable type.
 * @param TEvent The type representing the events that trigger state transitions. Must be a non-nullable type.
 * @param logger The logger instance used for outputting formatted state transition information.
 * @param logTag A string tag used to categorize log messages from this debugger.
 * @param clock A clock instance used to timestamp state transition records.
 * @param valueFormatter A function that formats values for display, taking the value and a default formatter
 *  function as parameters.
 */
internal class StateMachineDebugger<TState : Any, TEvent : Any>(
    logger: Logger,
    logTag: String?,
    @OptIn(ExperimentalTime::class)
    private val clock: Clock,
    val valueFormatter: (Any, formatter: (Any) -> String) -> String,
) {
    /**
     * A mutable collection that stores the complete history of state transitions for debugging purposes.
     *
     * This stack maintains a chronological record of all state transitions that have occurred in the
     * state machine being debugged. Each entry contains detailed information about a transition,
     * including the transition object, the triggering event, the previous state, the new state,
     * and the timestamp when the transition occurred.
     *
     * The stack is implemented as an ArrayDeque to provide efficient addition of new records as
     * transitions occur during state machine execution. Records are added through the record method
     * and can be formatted and displayed via the dump method for debugging and analysis.
     */
    val historyStack: List<StateMachine.StateTransitionRecord<TState, TEvent>> get() = _historyStack.toList()
    private val _historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TState, TEvent>>()

    @OptIn(ExperimentalTime::class)
    private val statePrettyPrinter: StatePrettyPrinter<TState, TEvent> =
        StatePrettyPrinter(logger, logTag, clock, valueFormatter)

    fun record(
        transition: StateMachine.Transition<TState, TEvent>,
        event: TEvent,
        previousState: TState,
        newState: TState,
    ) {
        @OptIn(ExperimentalTime::class)
        val record = StateMachine.StateTransitionRecord(
            transition = transition,
            event = event,
            previousState = previousState,
            newState = newState,
            timestamp = clock.now(),
        )
        _historyStack.addLast(record)
    }

    /**
     * Outputs a formatted representation of the recorded state transition history.
     *
     * This method delegates to the state pretty printer to display all state transitions
     * that have been recorded in the history stack. The output format is determined by
     * the pretty printer implementation and typically includes details such as transition
     * types, events, state changes, and timestamps in a human-readable format.
     *
     * This is useful for debugging state machine behavior by visualizing the sequence
     * of state transitions that have occurred during execution.
     */
    fun dump() {
        statePrettyPrinter.prettyPrint(_historyStack)
    }
}
