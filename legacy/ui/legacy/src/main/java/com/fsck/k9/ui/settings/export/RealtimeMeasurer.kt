package com.fsck.k9.ui.settings.export

fun interface RealtimeMeasurer {
    suspend fun measure(block: suspend () -> Unit): Long
}
