package net.thunderbird.core.android.logging

import org.koin.dsl.module

val loggingModule = module {
    factory<ProcessExecutor> { RealProcessExecutor() }
    factory<LogFileWriter> {
        MultiLogFileWriter(
            contentResolver = get(),
            processExecutor = get(),
            context = get(),
        )
    }
}
