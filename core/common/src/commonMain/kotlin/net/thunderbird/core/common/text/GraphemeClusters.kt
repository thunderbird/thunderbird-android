package net.thunderbird.core.common.text

/**
 * Grapheme cluster (user-perceived character) operations on text.
 *
 * Unlike [String.length] and [String.take], which work on UTF-16 code units, these functions treat a
 * multi-code-point character such as an emoji (including ZWJ sequences like 🐦‍🔥) as a single character.
 */
object GraphemeClusters {

    /**
     * Returns the number of grapheme clusters in [text].
     */
    fun count(text: String): Int = text.graphemeClusterBoundaries().lastIndex

    /**
     * Returns the first [count] grapheme clusters of [text], or the whole [text] if it contains fewer than [count]
     * clusters.
     */
    fun take(text: String, count: Int): String {
        val boundaries = text.graphemeClusterBoundaries()
        return text.substring(0, boundaries[count.coerceIn(0, boundaries.lastIndex)])
    }
}

/**
 * Returns the grapheme cluster boundaries of this string, starting with `0` and ending with [String.length].
 *
 * Platform implementations should use the platform's own text segmentation, so the result follows the Unicode
 * version supported by the platform.
 */
internal expect fun String.graphemeClusterBoundaries(): List<Int>
