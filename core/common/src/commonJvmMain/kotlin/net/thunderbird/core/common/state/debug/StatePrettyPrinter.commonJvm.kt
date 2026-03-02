package net.thunderbird.core.common.state.debug

import androidx.annotation.VisibleForTesting
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import net.thunderbird.core.common.extension.mapOrDefault
import net.thunderbird.core.common.state.StateMachine
import net.thunderbird.core.logging.Logger

private const val TAG = "StatePrettyPrinter"
internal const val MAX_COLLECTION_SIZE_PRINT_THRESHOLD = 5

@OptIn(ExperimentalTime::class)
internal actual fun <TState : Any, TEvent : Any> StatePrettyPrinter(
    logger: Logger,
    logTag: String?,
    clock: Clock,
    valueFormatter: (Any, formatter: (Any) -> String) -> String,
): StatePrettyPrinter<TState, TEvent> = CommonJvmStatePrettyPrinter(logger, logTag, clock, valueFormatter)

/**
 * A pretty printer implementation for state machine debugging that formats and logs state transitions.
 *
 * This class handles only the logging orchestration (when and what to log). Formatting is delegated
 * to [TransitionFormatter], diffing to [StateDiffer], and value formatting to [ValueFormatter].
 */
@OptIn(ExperimentalTime::class)
@VisibleForTesting
internal class CommonJvmStatePrettyPrinter<TState : Any, TEvent : Any>(
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
        val record = historyStack.last()
        val dump = buildString {
            appendLine(StatePrettyPrinterVocabulary.buildLatestTransitionMarker(logTag))
            val previousTimestamp = calculatePreviousTimestamp(historyStack, record)
            with(formatter) { appendTransitionRecord(record, previousTimestamp = previousTimestamp) }
        }
        logger.verbose(logTag) { dump }
    }

    private fun logFullDumpIfCooldownElapsed(
        historyStack: ArrayDeque<StateMachine.StateTransitionRecord<TState, TEvent>>,
    ) {
        val now = clock.now()
        // Avoid dumping the full history when the first state transition happens. This avoids
        // unnecessary duplicated logging.
        if (historyStack.size <= 1 || now - lastFullDumpTimestamp < fullDumpCooldown) return
        lastFullDumpTimestamp = now

        val dump = buildString {
            appendDump {
                var previousTimestamp: Instant? = null
                for ((index, record) in historyStack.withIndex()) {
                    if (index > 0) {
                        appendLine(StatePrettyPrinterVocabulary.STATE_HISTORY_STATE_SEPARATOR)
                    }
                    with(formatter) { appendTransitionRecord(record, previousTimestamp) }
                    previousTimestamp = record.timestamp
                }
            }
        }
        logger.verbose(logTag) { dump }
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
        record: StateMachine.StateTransitionRecord<TState, TEvent>,
    ): Instant? {
        val index = historyStack.indexOf(record)
        return if (index == 0) null else historyStack[index - 1].timestamp
    }
}
