package net.thunderbird.core.common.state.debug

/**
 * Holds contextual information used during state diff formatting.
 *
 * @param indentSize The current indentation level (in spaces).
 * @param isStateClassChanged Whether the state class itself changed (not just its properties).
 */
internal data class DiffContext(
    val indentSize: Int,
    val isStateClassChanged: Boolean,
) {
    /**
     * Creates a new DiffContext with increased indentation level.
     *
     * This method is used to create nested context when formatting hierarchical state diffs,
     * allowing proper visual representation of nested object structures.
     *
     * @param additionalIndent The number of spaces to add to the current indentation level (default: 2).
     * @return A new DiffContext instance with the increased indentation size.
     */
    fun nested(additionalIndent: Int = 2): DiffContext = copy(indentSize = indentSize + additionalIndent)
}
