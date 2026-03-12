package net.thunderbird.core.common.state.debug

import androidx.annotation.VisibleForTesting
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import net.thunderbird.core.common.extension.mapOrDefault
import net.thunderbird.core.common.state.StateMachine
import net.thunderbird.core.logging.Logger

internal const val MAX_COLLECTION_SIZE_PRINT_THRESHOLD = 5
private const val TAG = "StatePrettyPrinter"

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
internal fun <TState : Any, TEvent : Any> StatePrettyPrinter(
    logger: Logger,
    logTag: String?,
    clock: Clock,
    valueFormatter: (Any, formatter: (Any) -> String) -> String,
): StatePrettyPrinter<TState, TEvent> =
    CommonStatePrettyPrinter(logger, logTag, clock, valueFormatter)

/**
 * A pretty printer implementation for state machine debugging that formats and logs state transitions.
 *
 * This class handles only the logging orchestration (when and what to log). Formatting is delegated
 * to [TransitionFormatter], diffing to [StateDiffer], and value formatting to [ValueFormatter].
 */
@VisibleForTesting
internal class CommonStatePrettyPrinter<TState : Any, TEvent : Any>(
    private val logger: Logger,
    logTag: String?,
    private val clock: Clock,
    customValueFormatter: (Any, formatter: (Any) -> String) -> String,
    private val fullDumpCooldown: Duration = 2.seconds,
) : StatePrettyPrinter<TState, TEvent> {
    private val logTag = "${logTag.mapOrDefault { "$it$" }}$TAG"

    private val valueFormatter = ValueFormatter(customValueFormatter)
    private val differ = StateDiffer(valueFormatter)
    private val formatter = TransitionFormatter<TState, TEvent>(valueFormatter, differ)

    private var lastFullDumpTimestamp: Instant = Instant.DISTANT_PAST

    override fun prettyPrint(historyStack: ArrayDeque<StateMachine.StateTransitionRecord<TState, TEvent>>) {
        if (historyStack.isEmpty()) {
            logger.verbose(logTag) {
                buildString {
                    appendDump {
                        appendLine(StatePrettyPrinterVocabulary.STATE_HISTORY_EMPTY)
                    }
                }
            }
            return
        }

        logLastTransition(historyStack)
        logFullDumpIfCooldownElapsed(historyStack)
    }

    private fun logLastTransition(
        historyStack: ArrayDeque<StateMachine.StateTransitionRecord<TState, TEvent>>,
    ) {
        logger.verbose(logTag) {
            val record = historyStack.last()
            buildString {
                appendLine(StatePrettyPrinterVocabulary.buildLatestTransitionMarker(logTag))
                val previousTimestamp = calculatePreviousTimestamp(historyStack)
                append(formatter.buildTransitionRecord(record, previousTimestamp = previousTimestamp))
            }
        }
    }

    private fun logFullDumpIfCooldownElapsed(
        historyStack: ArrayDeque<StateMachine.StateTransitionRecord<TState, TEvent>>,
    ) {
        val now = clock.now()
        // Avoid dumping the full history when the first state transition happens. This avoids
        // unnecessary duplicated logging.
        if (historyStack.size <= 1 || now - lastFullDumpTimestamp < fullDumpCooldown) return
        lastFullDumpTimestamp = now

        logger.verbose(logTag) {
            buildString {
                appendDump {
                    var previousTimestamp: Instant? = null
                    for ((index, record) in historyStack.withIndex()) {
                        if (index > 0) {
                            appendLine(StatePrettyPrinterVocabulary.STATE_HISTORY_STATE_SEPARATOR)
                        }
                        append(formatter.buildTransitionRecord(record, previousTimestamp))
                        previousTimestamp = record.timestamp
                    }
                }
            }
        }
    }

    private fun StringBuilder.appendDump(content: StringBuilder.() -> Unit) {
        appendLine(StatePrettyPrinterVocabulary.STATE_HISTORY_DUMP_TITLE)
        appendLine(StatePrettyPrinterVocabulary.STATE_HISTORY_DUMP_BEGIN)
        appendLine(StatePrettyPrinterVocabulary.STATE_HISTORY_STATE_SEPARATOR)
        content()
        appendLine(StatePrettyPrinterVocabulary.STATE_HISTORY_STATE_SEPARATOR)
        appendLine(StatePrettyPrinterVocabulary.STATE_HISTORY_DUMP_END)
    }

    private fun calculatePreviousTimestamp(
        historyStack: ArrayDeque<StateMachine.StateTransitionRecord<TState, TEvent>>,
    ): Instant? {
        val index = historyStack.lastIndex
        return if (index == 0) null else historyStack[index - 1].timestamp
    }
}

/**
 * Converts this object to a map of property names to their current values using reflection.
 *
 * This function extracts all non-static, non-synthetic fields from the object and returns them
 * as a map where keys are field names and values are the current field values. Collections and
 * Maps are treated specially and return an empty map to avoid recursive property extraction.
 *
 * The fields are sorted by name to ensure consistent ordering across invocations.
 *
 * @return A map containing field names as keys and their corresponding values, or an empty map
 * if this object is a Collection or Map. Field values may be null.
 */
internal expect fun <T : Any> T.toPropertyMap(): Map<String, Any?>
