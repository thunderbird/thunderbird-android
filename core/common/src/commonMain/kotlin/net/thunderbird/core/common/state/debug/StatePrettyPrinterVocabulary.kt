package net.thunderbird.core.common.state.debug

import kotlin.reflect.KClass
import net.thunderbird.core.common.extension.mapOrDefault
import net.thunderbird.core.common.state.debug.StatePrettyPrinterVocabulary.STATE_CHANGE_MARKER
import net.thunderbird.core.common.state.debug.extension.prependIndent

/**
 * Provides constants and formatting functions used by [StatePrettyPrinter] to produce
 * human-readable representations of state machine transitions and history dumps.
 */
internal object StatePrettyPrinterVocabulary {
    const val STATE_CHANGE_MARKER = "->"
    const val STATE_NO_CHANGE_MARKER = "⟳"
    const val STATE_DATA_NO_CHANGES = "No changes"
    const val STATE_HISTORY_DUMP_TITLE = "══ State machine history dump ══"
    const val STATE_HISTORY_DUMP_BEGIN = "BEGINNING OF HISTORY DUMP:"
    const val STATE_HISTORY_DUMP_END = "END OF HISTORY DUMP"
    const val STATE_HISTORY_EMPTY = "No state transitions to print"
    const val STATE_HISTORY_STATE_SEPARATOR = "---"
    const val VALUE_TRUNCATION_MARKER = "..."

    /**
     * Builds a decorated header line for the latest transition log entry.
     */
    fun buildLatestTransitionMarker(logTag: String): String = "── $logTag: Latest transition ──"

    /**
     * Formats a value that changed between transitions, showing old and new values
     * separated by [STATE_CHANGE_MARKER].
     *
     * @param oldValue the previous value representation.
     * @param newValue the new value representation.
     * @param indentSize number of spaces to indent the output.
     * @param key optional property name to prefix the output.
     */
    fun formatValueWithChange(oldValue: String, newValue: String, indentSize: Int, key: String? = null): String =
        "${key.mapOrDefault { "$it: " }}$oldValue $STATE_CHANGE_MARKER $newValue".prependIndent(size = indentSize)

    /**
     * Formats a value that remained unchanged between transitions.
     *
     * @param oldValue the unchanged value representation.
     * @param indentSize number of spaces to indent the output.
     * @param key optional property name to prefix the output.
     */
    fun formatValueNoChanges(oldValue: String, indentSize: Int, key: String? = null): String =
        "${key.mapOrDefault { "$it: " }}$oldValue ($STATE_DATA_NO_CHANGES)".prependIndent(size = indentSize)

    /**
     * Formats an object-level property that has no changes, displaying its type name.
     *
     * @param type the [KClass] of the object, or `null` if unknown.
     * @param key the property name.
     * @param indentSize number of spaces to indent the output.
     */
    fun formatObjectNoChanges(type: KClass<*>?, key: String, indentSize: Int): String =
        "$key: ${type?.simpleName ?: "null "}($STATE_DATA_NO_CHANGES)".prependIndent(size = indentSize)

    /**
     * Formats a collection size summary (e.g. `[3 items]`).
     */
    fun formatCollectionItemsValue(size: Int): String =
        "[$size items]"

    /**
     * Formats a map size summary (e.g. `{3 entries}`).
     */
    fun formatMapEntriesValue(size: Int): String =
        "{$size entries}"

    /**
     * Formats a map entry as `[key]: value`.
     */
    fun formatEntryKeyValue(key: String, value: String): String =
        "[$key]: $value"

    /**
     * Formats an iterable item that has not changed, using bracket-wrapped [key] as the label.
     */
    fun formatIterableItemNoChange(key: Any, oldValue: String, indentSize: Int): String =
        formatValueNoChanges(
            key = "[$key]",
            oldValue = oldValue,
            indentSize = indentSize,
        )

    /**
     * Formats an iterable item that has changed, using bracket-wrapped [key] as the label.
     */
    fun formatIterableItemWithChange(key: Any, oldValue: String, newValue: String, indentSize: Int): String =
        formatValueWithChange(
            key = "[$key]",
            oldValue = oldValue,
            newValue = newValue,
            indentSize = indentSize,
        )

    /**
     * Formats a collection whose size changed but is too large to diff element-by-element.
     */
    fun formatLongCollectionWithChanges(key: String, oldSize: Int, newSize: Int, indentSize: Int): String =
        formatValueWithChange(
            key = key,
            oldValue = "[$oldSize items]",
            newValue = "[$newSize items]",
            indentSize = indentSize,
        )

    /**
     * Formats a collection that remained the same, showing its size.
     */
    fun buildListSameString(key: String, size: Int, indentSize: Int): String =
        formatValueNoChanges(key = key, oldValue = "[$size items]", indentSize = indentSize)

    /**
     * Formats a map whose size changed but is too large to diff entry-by-entry.
     */
    fun formatLongMapWithChanges(key: String, oldSize: Int, newSize: Int, indentSize: Int): String =
        formatValueWithChange(
            key = key,
            oldValue = "{$oldSize entries}",
            newValue = "{$newSize entries}",
            indentSize = indentSize,
        )

    /**
     * Formats a map that remained the same, showing its size.
     */
    fun buildMapSameString(key: String, size: Int, indentSize: Int): String =
        formatValueNoChanges(key = key, oldValue = "{$size entries}", indentSize = indentSize)
}
