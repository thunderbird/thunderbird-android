package net.thunderbird.app.common

import kotlinx.coroutines.runBlocking
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.components.core.logging.file.FileLogSink

private const val TAG = "ExceptionHandler"

internal class ExceptionHandler(
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
    private val logger: Logger,
    private val syncDebugFileLogSink: FileLogSink,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            logger.error(tag = TAG, throwable = e) { "UncaughtException" }
            runBlocking {
                syncDebugFileLogSink.flush()
            }
        } finally {
            defaultHandler?.uncaughtException(t, e)
        }
    }
}
