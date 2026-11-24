package net.thunderbird.core.logging.internal

import net.thunderbird.core.logging.LogcatExporter
import org.koin.dsl.module

val coreLoggingAndroidModule = module {
    factory<ProcessExecutor> { RealProcessExecutor() }
    factory<LogcatExporter> {
        AndroidLogcatExporter(
            contentResolver = get(),
            processExecutor = get(),
        )
    }
}
