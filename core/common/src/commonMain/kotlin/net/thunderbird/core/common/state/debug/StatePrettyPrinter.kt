package net.thunderbird.core.common.state.debug

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import net.thunderbird.core.common.state.StateMachine
import net.thunderbird.core.logging.Logger

/**
 * Defines a contract for formatting and outputting the state transition history of a state machine
 * in a human-readable format.
 *
 * This interface provides a mechanism to visualize the sequence of state transitions that have
 * occurred within a state machine. It is primarily used for debugging, logging, and monitoring
 * purposes, enabling developers to understand the flow of state changes and the events that
 * triggered them.
 *
 * Implementations of this interface determine the specific formatting style and output destination
 * for the state transition history.
 *
 * @param TState The base type for all possible states in the state machine.
 * @param TEvent The base type for all possible events in the state machine.
 */
internal interface StatePrettyPrinter<TState : Any, TEvent : Any> {

    /**
     * Formats and outputs the state transition history in a human-readable format.
     *
     * This method processes a collection of state transition records and presents them
     * in a way that is easy to read and understand. The specific formatting style and
     * output destination (such as console, log file, or string buffer) are determined
     * by the implementation.
     *
     * The history stack typically contains transitions in chronological order, allowing
     * for sequential visualization of how the state machine evolved over time in response
     * to various events.
     *
     * @param historyStack A deque containing the state transition records to be formatted
     * and displayed. Each record includes information about the transition, the triggering
     * event, the previous state, the new state, and the timestamp of the transition.
     */
    fun prettyPrint(historyStack: ArrayDeque<StateMachine.StateTransitionRecord<TState, TEvent>>)
}

/**
 * Creates a platform-specific implementation of StatePrettyPrinter for formatting and logging
 * state machine transition history.
 *
 * This factory function provides a platform-appropriate implementation that can format state
 * transitions in a human-readable format and output them using the provided logger. The printer
 * uses the given clock for timestamp information and applies custom formatting through the
 * valueFormatter parameter.
 *
 * @param logger The Logger instance used to output the formatted state transition history.
 * @param logTag An optional tag string to categorize or identify the log messages produced
 * by the pretty printer.
 * @param clock The Clock instance used to obtain timing information for state transitions.
 * @param valueFormatter A function that formats arbitrary values into string representations.
 * It receives the value to format and a default formatter function, allowing for custom
 * formatting logic while falling back to default formatting when needed.
 * @param TState The base type for all possible states in the state machine.
 * @param TEvent The base type for all possible events in the state machine.
 * @return A platform-specific implementation of StatePrettyPrinter configured with the
 * provided parameters.
 */
@OptIn(ExperimentalTime::class)
internal expect fun <TState : Any, TEvent : Any> StatePrettyPrinter(
    logger: Logger,
    logTag: String?,
    clock: Clock,
    valueFormatter: (Any, formatter: (Any) -> String) -> String,
): StatePrettyPrinter<TState, TEvent>

/**
 * Converts this object to a map of property names to their current values using reflection.
 *
 * This function extracts all non-static, non-synthetic fields from the object and returns them
 * as a map where keys are field names and values are the current field values. Collections and
 * Maps are treated specially and return an empty map to avoid recursive property extraction.
 *
 * The fields are sorted by name to ensure consistent ordering across invocations.
 *
 * **NOTE**: We should, technically, be able to have this directly in the `commonJvm` module,
 *           but for some reason, maybe an AGP bug, the `::class.java` shows as error in the
 *           IDE, although the code compiles and run fine. To avoid the annoyance, currently
 *           the code is duplicated in the `jvm` and `android` sourceSets.
 *
 *           TODO: We should review this after the AGP 9 update happens.
 *
 * @return A map containing field names as keys and their corresponding values, or an empty map
 * if this object is a Collection or Map. Field values may be null.
 */
internal expect fun <T : Any> T.toPropertyMap(): Map<String, Any?>
