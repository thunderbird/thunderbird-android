package net.thunderbird.core.common.text.compat

/**
 * Corrects grapheme cluster boundaries produced by a [java.text.BreakIterator] that predates the Unicode 9 emoji
 * rules, such as the ICU on Android 7.x (API 24/25) or JDKs before 20.
 *
 * These implementations split ZWJ sequences, skin tone modifiers and tag sequences, and merge runs of regional
 * indicators into a single cluster.
 *
 * Remove once minSdk is 26 or higher.
 */
internal object GraphemeClusterBoundariesCompat {

    /**
     * Returns the corrected [boundaries] of [text], so that emoji sequences are kept together and regional
     * indicators are paired into flags.
     */
    fun correct(text: String, boundaries: List<Int>): List<Int> {
        val joined = boundaries.filterIndexed { index, boundary ->
            index == 0 || index == boundaries.lastIndex || !text.isJoinedAt(boundary)
        }

        return buildList {
            add(joined.first())
            joined.zipWithNext { start, end ->
                addAll(text.regionalIndicatorPairBoundaries(start, end))
                add(end)
            }
        }
    }

    /**
     * Whether the clusters before and after [index] belong to the same emoji sequence.
     */
    private fun String.isJoinedAt(index: Int): Boolean {
        val codePoint = codePointAt(index)
        return this[index - 1] == ZERO_WIDTH_JOINER ||
            this[index] == ZERO_WIDTH_JOINER ||
            codePoint in EMOJI_MODIFIERS ||
            codePoint in TAGS
    }

    /**
     * Returns the boundaries between pairs of regional indicators at the start of the cluster from [start] to
     * [end].
     */
    private fun String.regionalIndicatorPairBoundaries(start: Int, end: Int): List<Int> = buildList {
        var index = start
        var count = 0
        while (index < end) {
            val codePoint = codePointAt(index)
            if (codePoint !in REGIONAL_INDICATORS) break
            if (count > 0 && count % 2 == 0) add(index)
            count++
            index += Character.charCount(codePoint)
        }
    }

    private const val ZERO_WIDTH_JOINER = '‍'
    private val EMOJI_MODIFIERS = 0x1F3FB..0x1F3FF
    private val TAGS = 0xE0020..0xE007F
    private val REGIONAL_INDICATORS = 0x1F1E6..0x1F1FF
}
