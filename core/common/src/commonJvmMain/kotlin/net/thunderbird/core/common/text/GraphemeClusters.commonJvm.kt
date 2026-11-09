package net.thunderbird.core.common.text

import java.text.BreakIterator

internal actual fun String.graphemeClusterBoundaries(): List<Int> {
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
