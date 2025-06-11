package app.k9mail.core.common.exception

import kotlinx.coroutines.runBlocking
import net.thunderbird.core.logging.file.FileLogSink
import net.thunderbird.core.logging.legacy.Log
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class ExceptionHandler(
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler, KoinComponent {
    private val syncDebugFileLogSink: FileLogSink by inject(named("syncDebug"))

    override fun uncaughtException(t: Thread, e: Throwable) {
        Log.e("UncaughtException", e.toString(), e)
        runBlocking {
            syncDebugFileLogSink.flushAndCloseBuffer()
        }
        defaultHandler?.uncaughtException(t, e)
    }
}
