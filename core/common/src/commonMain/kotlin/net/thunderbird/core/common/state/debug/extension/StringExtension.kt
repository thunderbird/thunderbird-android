package net.thunderbird.core.common.state.debug.extension

/**
 * Returns a new string with each line prepended by the specified number of spaces.
 *
 * This extension function adds indentation to the string by prepending a specified
 * number of space characters to the beginning. It's used to format debug output
 * with proper indentation levels for hierarchical state diff visualization.
 *
 * @param size The number of spaces to prepend to the string.
 * @return A new string with the specified indentation applied.
 */
internal fun String.prependIndent(size: Int): String = prependIndent(" ".repeat(size))
