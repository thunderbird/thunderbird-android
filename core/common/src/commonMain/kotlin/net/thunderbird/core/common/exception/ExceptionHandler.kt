package net.thunderbird.core.common.exception

import kotlinx.coroutines.runBlocking
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.file.FileLogSink
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class ExceptionHandler(
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
    private val logger: Logger,
) : Thread.UncaughtExceptionHandler, KoinComponent {
    private val syncDebugFileLogSink: FileLogSink by inject(named("syncDebug"))

    override fun uncaughtException(t: Thread, e: Throwable) {
        logger.error(throwable = e) { "UncaughtException" }

        runBlocking {
            syncDebugFileLogSink.flushAndCloseBuffer()
        }
        defaultHandler?.uncaughtException(t, e)
    }
}
