package com.fsck.k9.ui.settings.export

import android.os.SystemClock

class AndroidRealtimeMeasurer : RealtimeMeasurer {
    override suspend fun measure(block: suspend () -> Unit): Long {
        val start = SystemClock.elapsedRealtime()
        block()
        return SystemClock.elapsedRealtime() - start
    }
}
