package net.thunderbird.app.common.core

import android.content.Context
import net.thunderbird.app.common.BuildConfig
import net.thunderbird.core.logging.DefaultLogger
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogSink
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.composite.CompositeLogSink
import net.thunderbird.core.logging.console.ConsoleLogSink
import net.thunderbird.core.logging.file.AndroidFileSystemManager
import net.thunderbird.core.logging.file.FileLogSink
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appCommonCoreModule: Module = module {
    single<LogLevel> {
        if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.INFO
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

    single<CompositeLogSink>(named(SYNC_DEBUG_LOG)) {
        CompositeLogSink(
            level = get(),
            sinks = get(),
        )
    }

    single<FileLogSink>(named(SYNC_DEBUG_LOG)) {
        FileLogSink(
            level = LogLevel.DEBUG,
            fileName = "thunderbird-sync-debug",
            fileLocation = get<Context>().filesDir.path,
            fileSystemManager = AndroidFileSystemManager(get<Context>().contentResolver),
        )
    }

    single<Logger> (named(SYNC_DEBUG_LOG)) {
        DefaultLogger(
            sink = get<CompositeLogSink>(named(SYNC_DEBUG_LOG)),
        )
    }
}

internal const val SYNC_DEBUG_LOG = "syncDebug"
