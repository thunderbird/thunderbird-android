package net.thunderbird.core.common.text

import net.thunderbird.core.common.text.compat.GraphemeClusterBoundariesCompat

internal actual fun String.graphemeClusterBoundaries(): List<Int> {
    val boundaries = breakIteratorBoundaries()
    return if (Runtime.version().feature() >= JDK_WITH_EMOJI_SEGMENTATION) {
        boundaries
    } else {
        GraphemeClusterBoundariesCompat.correct(this, boundaries)
    }
}

private const val JDK_WITH_EMOJI_SEGMENTATION = 20
