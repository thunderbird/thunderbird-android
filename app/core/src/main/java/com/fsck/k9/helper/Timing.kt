package com.fsck.k9.helper

import android.os.SystemClock

/**
 * Executes the given [block] and returns elapsed realtime in milliseconds.
 */
inline fun measureRealtimeMillis(block: () -> Unit): Long {
    val start = SystemClock.elapsedRealtime()
    block()
    return SystemClock.elapsedRealtime() - start
}
