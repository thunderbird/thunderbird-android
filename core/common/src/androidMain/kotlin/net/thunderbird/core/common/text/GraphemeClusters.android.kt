package net.thunderbird.core.common.text

import android.os.Build
import net.thunderbird.core.common.text.compat.GraphemeClusterBoundariesCompat

internal actual fun String.graphemeClusterBoundaries(): List<Int> {
    val boundaries = breakIteratorBoundaries()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        boundaries
    } else {
        GraphemeClusterBoundariesCompat.correct(this, boundaries)
    }
}
