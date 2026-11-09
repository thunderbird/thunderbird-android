package net.thunderbird.core.common.text

import java.text.BreakIterator

/**
 * Returns the grapheme cluster boundaries of this string as reported by the platform's [BreakIterator], starting
 * with `0` and ending with [String.length].
 */
internal fun String.breakIteratorBoundaries(): List<Int> {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(this)

    return buildList {
        var boundary = iterator.first()
        while (boundary != BreakIterator.DONE) {
            add(boundary)
            boundary = iterator.next()
        }
    }
}
