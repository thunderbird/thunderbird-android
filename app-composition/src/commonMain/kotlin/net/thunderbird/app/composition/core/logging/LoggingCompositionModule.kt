package net.thunderbird.app.composition.core.logging

import kotlin.time.ExperimentalTime
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import net.thunderbird.core.common.inject.getList
import net.thunderbird.core.common.inject.singleListOf
import net.thunderbird.core.file.DirectoryProvider
import net.thunderbird.core.logging.DefaultLogger
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogLevelManager
import net.thunderbird.core.logging.LogLevelProvider
import net.thunderbird.core.logging.LogSink
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.composite.CompositeLogSink
import net.thunderbird.core.logging.config.InMemoryLogLevelManager
import net.thunderbird.core.logging.console.ConsoleLogSink
import net.thunderbird.core.logging.file.FileLogSink
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

internal val loggingCompositionModule = module {
    single<LogLevelManager> {
        val defaultLevel = if (get<PlatformConfigProvider>().isDebug) LogLevel.VERBOSE else LogLevel.INFO

        InMemoryLogLevelManager(defaultLevel)
    }.bind<LogLevelProvider>()

    singleListOf<LogSink>(
        { ConsoleLogSink(level = LogLevel.VERBOSE) },
    )

    single<CompositeLogSink> {
        CompositeLogSink(
            logLevelProvider = get(),
            sinks = getList(),
        )
    }

    single<Logger> {
        @OptIn(ExperimentalTime::class)
        DefaultLogger(
            sink = get<CompositeLogSink>(),
        )
    }

    // Define this list lazily to avoid eager initialization at app startup
    single<List<LogSink>>(qualifier = named(SYNC_DEBUG_LOG), createdAtStart = false) {
        listOf(get<FileLogSink>(named(SYNC_DEBUG_LOG)))
    }

    single<CompositeLogSink>(named(SYNC_DEBUG_LOG)) {
        CompositeLogSink(
            logLevelProvider = get(),
            sinks = get<List<LogSink>>(named(SYNC_DEBUG_LOG)),
        )
    }

    single<FileLogSink>(named(SYNC_DEBUG_LOG)) {
        FileLogSink(
            level = LogLevel.DEBUG,
            fileName = "thunderbird-sync-debug",
            fileLocation = requireNotNull(get<DirectoryProvider>().getFilesDir().path),
            fileManager = get(),
        )
    }

    single<Logger>(named(SYNC_DEBUG_LOG)) {
        @OptIn(ExperimentalTime::class)
        DefaultLogger(
            sink = get<CompositeLogSink>(named(SYNC_DEBUG_LOG)),
        )
    }
}

private const val SYNC_DEBUG_LOG = "syncDebug"
