package net.thunderbird.core.logging.internal

import net.thunderbird.components.core.logging.DefaultLogger
import net.thunderbird.components.core.logging.LogLevel
import net.thunderbird.components.core.logging.LogLevelProvider
import net.thunderbird.components.core.logging.LogSink
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.components.core.logging.LoggingControl
import net.thunderbird.components.core.logging.ToggleableLogger
import net.thunderbird.components.core.logging.composite.CompositeLogSink
import net.thunderbird.components.core.logging.console.ConsoleLogSink
import net.thunderbird.components.core.logging.file.FileLogSink
import net.thunderbird.core.common.inject.getList
import net.thunderbird.core.common.inject.singleListOf
import net.thunderbird.core.logging.DebugLogConfigurator
import net.thunderbird.core.logging.SyncDebugLogExporter
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module

const val DEFAULT_LOGGING_ENABLED = "defaultLoggingEnabled"
const val SYNC_DEBUG_LOG = "syncDebug"

val coreLoggingModule = module {
    single<LogLevelProvider> { LogLevelProvider { get<LogLevel>() } }

    singleListOf<LogSink>(
        { ConsoleLogSink(level = LogLevel.VERBOSE) },
    )

    single<CompositeLogSink> {
        CompositeLogSink(
            logLevelProvider = get(),
            sinks = getList(),
        )
    }

    single {
        ToggleableLogger(
            delegate = DefaultLogger(sink = get<CompositeLogSink>()),
            enabled = get<Boolean>(named(DEFAULT_LOGGING_ENABLED)),
        )
    }.binds(arrayOf(Logger::class, LoggingControl::class))

    single<SyncDebugLogExporter> {
        DefaultSyncDebugLogExporter(
            fileLogSink = get<FileLogSink>(named(SYNC_DEBUG_LOG)),
            destinationFile = ::uriToPlatformFile,
        )
    }

    single<DebugLogConfigurator> {
        DefaultDebugLogConfigurator(
            syncDebugCompositeSink = get<CompositeLogSink>(named(SYNC_DEBUG_LOG)),
            syncDebugFileLogSink = get<FileLogSink>(named(SYNC_DEBUG_LOG)),
        )
    }
}
