package net.thunderbird.core.common.state.debug

import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import net.thunderbird.core.common.state.StateMachine

/**
 * Formats state transition records into human-readable strings.
 *
 * Responsible for building the textual representation of individual transitions
 * and full history dumps. Delegates diff computation to [StateDiffer] and
 * value formatting to [ValueFormatter].
 */
@OptIn(ExperimentalTime::class)
internal class TransitionFormatter<TState : Any, TEvent : Any>(
    private val valueFormatter: ValueFormatter,
    private val differ: StateDiffer,
) {

    /**
     * Appends a formatted state transition record to this StringBuilder.
     *
     * Formats a single state transition into a human-readable multi-line string that
     * includes:
     * - A transition header showing the state change with an appropriate marker
     *   (-> for state class changes, ⟳ for same-class transitions)
     * - The triggering event class name
     * - Elapsed time since the previous transition (if available)
     * - Event payload properties (if present)
     * - A diff of the state data changes between the previous and new states
     *
     * The output uses indentation to organize information hierarchically and delegates to
     * [StateDiffer] for computing state differences and [ValueFormatter] for formatting
     * property values.
     *
     * @param record The state transition record to format, containing the previous state, new
     *  state, event, and metadata
     * @param previousTimestamp The timestamp of the previous transition, used to calculate elapsed
     *  time; null if this is the first transition
     */
    fun StringBuilder.appendTransitionRecord(
        record: StateMachine.StateTransitionRecord<TState, TEvent>,
        previousTimestamp: Instant?,
    ) {
        val prev = record.previousState
        val new = record.newState
        val isStateClassChanged = prev::class != new::class
        val transitionMarker = if (isStateClassChanged) {
            StatePrettyPrinterVocabulary.STATE_CHANGE_MARKER
        } else {
            StatePrettyPrinterVocabulary.STATE_NO_CHANGE_MARKER
        }
        val elapsed = previousTimestamp?.let { formatElapsed(record.timestamp - it) } ?: ""

        append("${prev::class.simpleName} $transitionMarker ${new::class.simpleName}")
        append("event=${record.event::class.simpleName}".indented(size = 2))
        if (elapsed.isNotEmpty()) append(elapsed.indented(size = 2))
        appendLine()

        val eventProps = record.event.toPropertyMap()
        if (eventProps.isNotEmpty()) {
            appendLine("${record.event::class.simpleName} payload:".indented(size = 2))
            for ((key, value) in eventProps) {
                appendLine("$key: ${valueFormatter.format(value)}".indented(size = 4))
            }
        }

        appendLine("${new::class.simpleName} updated data:".indented(size = 2))
        with(differ) {
            appendDiff(prev, new, DiffContext(indentSize = 4, isStateClassChanged = isStateClassChanged))
        }
    }

    private fun formatElapsed(duration: Duration): String {
        val ms = duration.inWholeMilliseconds
        return "+${ms}ms"
    }
}
