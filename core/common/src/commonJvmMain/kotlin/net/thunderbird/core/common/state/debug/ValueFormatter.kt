package net.thunderbird.core.common.state.debug

/**
 * Formats values for display in state machine debug output.
 *
 * Handles lists, maps, nulls, and arbitrary objects with truncation for long strings
 * and large collections.
 */
internal class ValueFormatter(
    private val customFormatter: (Any, formatter: (Any) -> String) -> String,
) {

    fun format(value: Any?, maxLen: Int = 80): String = when (value) {
        is Collection<*> if value.size > MAX_COLLECTION_SIZE_PRINT_THRESHOLD ->
            StatePrettyPrinterVocabulary.formatCollectionItemsValue(value.size)

        is Collection<*> -> formatList(value)
        is Map<*, *> if value.size > MAX_COLLECTION_SIZE_PRINT_THRESHOLD ->
            StatePrettyPrinterVocabulary.formatMapEntriesValue(value.size)

        is Map<*, *> -> formatMap(value)
        null -> "null"
        else -> {
            val formattedValue = customFormatter(value, ::format)
            if (formattedValue.length > maxLen) {
                formattedValue.take(maxLen - 1) + StatePrettyPrinterVocabulary.VALUE_TRUNCATION_MARKER
            } else {
                formattedValue
            }
        }
    }

    private fun formatList(list: Collection<*>): String = buildString {
        append("[")
        appendLine()
        list.forEachIndexed { index, item ->
            appendLine(
                StatePrettyPrinterVocabulary.formatEntryKeyValue(
                    key = format(index),
                    value = format(item),
                ).indented(size = 2),
            )
        }
        append("]")
    }

    private fun formatMap(map: Map<*, *>): String = buildString {
        append("{")
        appendLine()
        map.forEach { (key, item) ->
            appendLine(
                StatePrettyPrinterVocabulary.formatEntryKeyValue(
                    key = format(key),
                    value = format(item),
                ).indented(2),
            )
        }
        append("}")
    }
}
