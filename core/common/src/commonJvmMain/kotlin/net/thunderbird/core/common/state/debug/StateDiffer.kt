package net.thunderbird.core.common.state.debug

/**
 * Computes and formats diffs between old and new state objects.
 *
 * All output is appended to a [StringBuilder] using indentation and visual markers
 * to highlight changed, added, and unchanged properties.
 */
internal class StateDiffer(
    private val valueFormatter: ValueFormatter,
) {

    /**
     * Appends a formatted diff between two objects to this StringBuilder.
     *
     * Compares the old and new objects by converting them to property maps and recursively
     * diffing their properties. If both objects are null or have no properties, performs a
     * simple value comparison. Properties are processed in a distinct, consistent order.
     * Only differences are appended to the output with appropriate indentation.
     *
     * @param old The previous state object to compare from, or null if there was no previous value.
     * @param new The new state object to compare to, or null if the value was removed.
     * @param context The diff context containing indentation level and state change information.
     */
    fun StringBuilder.appendDiff(old: Any?, new: Any?, context: DiffContext) {
        if (old == null && new == null) return

        val oldProps = old?.toPropertyMap().orEmpty()
        val newProps = new?.toPropertyMap().orEmpty()

        if (oldProps.isEmpty() && newProps.isEmpty()) {
            val oldStr = valueFormatter.format(old)
            val newStr = valueFormatter.format(new)
            appendLine(
                if (oldStr != newStr) {
                    StatePrettyPrinterVocabulary.formatValueWithChange(
                        oldValue = oldStr,
                        newValue = newStr,
                        indentSize = context.indentSize,
                    )
                } else {
                    StatePrettyPrinterVocabulary.formatValueNoChanges(
                        oldValue = oldStr,
                        indentSize = context.indentSize,
                    )
                },
            )
        } else {
            val allKeys = (oldProps.keys + newProps.keys).toList().distinct()
            for (key in allKeys) {
                appendPropertyDiff(key, oldProps[key], newProps[key], context)
            }
        }
    }

    private fun StringBuilder.appendPropertyDiff(
        key: String,
        oldVal: Any?,
        newVal: Any?,
        context: DiffContext,
    ) {
        val oldIsObject = oldVal != null && oldVal.toPropertyMap().isNotEmpty()
        val newIsObject = newVal != null && newVal.toPropertyMap().isNotEmpty()

        when {
            oldIsObject && newIsObject -> appendObjectDiff(key, oldVal, newVal, context)
            oldVal is Map<*, *> && newVal is Map<*, *> ->
                appendMapDiff(key, oldVal, newVal, context)

            oldVal is Collection<*> && newVal is Collection<*> ->
                appendListDiff(key, oldVal, newVal, context)

            else -> appendScalarDiff(key, oldVal, newVal, context)
        }
    }

    private fun StringBuilder.appendScalarDiff(key: String, oldVal: Any?, newVal: Any?, context: DiffContext) {
        val oldStr = valueFormatter.format(oldVal)
        val newStr = valueFormatter.format(newVal)
        if (oldStr != newStr) {
            appendLine(
                StatePrettyPrinterVocabulary.formatValueWithChange(
                    key = key,
                    oldValue = oldStr,
                    newValue = newStr,
                    indentSize = context.indentSize,
                ),
            )
        } else if (context.isStateClassChanged) {
            appendLine(
                StatePrettyPrinterVocabulary.formatValueNoChanges(
                    key = key,
                    oldValue = oldStr,
                    indentSize = context.indentSize,
                ),
            )
        }
    }

    private fun StringBuilder.appendObjectDiff(key: String, oldVal: Any?, newVal: Any?, context: DiffContext) {
        if (oldVal == newVal) {
            if (context.isStateClassChanged) {
                appendLine(
                    StatePrettyPrinterVocabulary.formatObjectNoChanges(
                        type = oldVal?.let { it::class },
                        key = key,
                        indentSize = context.indentSize,
                    ),
                )
            }
            return
        }
        appendLine("$key:".indented(size = context.indentSize))
        appendDiff(oldVal, newVal, context.nested())
    }

    private fun StringBuilder.appendMapDiff(key: String, oldVal: Map<*, *>?, newVal: Map<*, *>?, context: DiffContext) {
        val oldMap = oldVal.orEmpty()
        val newMap = newVal.orEmpty()
        val oldSize = oldMap.size
        val newSize = newMap.size
        val allMapKeys = (oldMap.keys + newMap.keys).distinct()
        val hasChanges = allMapKeys.any { oldMap[it] != newMap[it] }
        val isLongMap = oldSize > MAX_COLLECTION_SIZE_PRINT_THRESHOLD || newSize > MAX_COLLECTION_SIZE_PRINT_THRESHOLD

        val diff = when {
            hasChanges && isLongMap ->
                StatePrettyPrinterVocabulary.formatLongMapWithChanges(
                    key = key,
                    oldSize = oldSize,
                    newSize = newSize,
                    indentSize = context.indentSize,
                )

            hasChanges -> appendEntryChanges(
                oldIterable = oldMap.iterator(),
                newIterable = newMap.iterator(),
                transformKeyIndexed = { _, entry -> entry.key.toString() },
                formatOldValue = { valueFormatter.format(it?.value) },
                formatNewValue = { valueFormatter.format(it?.value) },
                openingMarker = "{",
                closingMarker = "}",
                context = context,
            )

            context.isStateClassChanged -> StatePrettyPrinterVocabulary.buildMapSameString(
                key = key,
                size = oldSize,
                indentSize = context.indentSize,
            )

            else -> null
        }
        if (diff != null) {
            appendLine(diff)
        }
    }

    private fun StringBuilder.appendListDiff(
        key: String,
        oldVal: Collection<*>?,
        newVal: Collection<*>?,
        context: DiffContext,
    ) {
        val oldSize = oldVal?.size ?: 0
        val newSize = newVal?.size ?: 0
        val isLongList = oldSize > MAX_COLLECTION_SIZE_PRINT_THRESHOLD || newSize > MAX_COLLECTION_SIZE_PRINT_THRESHOLD
        val diff = when {
            oldSize != newSize && isLongList ->
                StatePrettyPrinterVocabulary.formatLongCollectionWithChanges(
                    key = key,
                    oldSize = oldSize,
                    newSize = newSize,
                    indentSize = context.indentSize,
                )

            oldSize != newSize -> {
                appendEntryChanges(
                    oldIterable = oldVal?.iterator(),
                    newIterable = newVal?.iterator(),
                    transformKeyIndexed = { index, _ -> index.toString() },
                    formatOldValue = { valueFormatter.format(it) },
                    formatNewValue = { valueFormatter.format(it) },
                    openingMarker = "[",
                    closingMarker = "]",
                    context = context,
                )
            }

            context.isStateClassChanged -> StatePrettyPrinterVocabulary.buildListSameString(
                key = key,
                size = oldSize,
                indentSize = context.indentSize,
            )

            else -> null
        }
        if (diff != null) {
            appendLine(diff)
        }
    }

    private fun <T> StringBuilder.appendEntryChanges(
        oldIterable: Iterator<T>?,
        newIterable: Iterator<T>?,
        transformKeyIndexed: (Int, T) -> String,
        formatOldValue: (T?) -> String,
        formatNewValue: (T?) -> String,
        openingMarker: String,
        closingMarker: String,
        context: DiffContext,
    ) {
        var current = 0
        appendLine(openingMarker.indented(size = context.indentSize))
        while (oldIterable?.hasNext() == true || newIterable?.hasNext() == true) {
            val oldValue = if (oldIterable?.hasNext() == true) oldIterable.next() else null
            val newValue = if (newIterable?.hasNext() == true) newIterable.next() else null
            val indentSize = context.indentSize + 2

            val formattedOldValue = formatOldValue(oldValue)
            appendLine(
                if (oldValue != newValue) {
                    StatePrettyPrinterVocabulary.formatIterableItemWithChange(
                        key = transformKeyIndexed(current++, requireNotNull(newValue ?: oldValue)),
                        oldValue = formattedOldValue,
                        newValue = formatNewValue(newValue),
                        indentSize = indentSize,
                    )
                } else {
                    StatePrettyPrinterVocabulary.formatIterableItemNoChange(
                        key = transformKeyIndexed(current++, requireNotNull(newValue ?: oldValue)),
                        oldValue = formattedOldValue,
                        indentSize = indentSize,
                    )
                },
            )
        }
        appendLine(closingMarker.indented(size = context.indentSize))
    }
}
