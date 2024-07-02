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

/**
 * Executes the given [block] and returns pair of elapsed realtime in milliseconds and result of the code block.
 */
inline fun <T> measureRealtimeMillisWithResult(block: () -> T): Pair<Long, T> {
    val start = SystemClock.elapsedRealtime()
    val result = block()
    val elapsedTime = SystemClock.elapsedRealtime() - start
    return elapsedTime to result
}
