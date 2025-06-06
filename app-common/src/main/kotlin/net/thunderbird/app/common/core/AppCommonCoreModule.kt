package net.thunderbird.app.common.core

import net.thunderbird.core.logging.DefaultLogger
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogSink
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.composite.CompositeLogSink
import net.thunderbird.core.logging.console.ConsoleLogSink
import org.koin.core.module.Module
import org.koin.dsl.module

val appCommonCoreModule: Module = module {
    single<LogLevel> {
        LogLevel.INFO
    }

    single<List<LogSink>> {
        listOf(
            ConsoleLogSink(
                level = get(),
            ),
        )
    }

    single<CompositeLogSink> {
        CompositeLogSink(
            level = get(),
            sinks = get(),
        )
    }

    single<Logger> {
        DefaultLogger(
            sink = get<CompositeLogSink>(),
        )
    }
}
