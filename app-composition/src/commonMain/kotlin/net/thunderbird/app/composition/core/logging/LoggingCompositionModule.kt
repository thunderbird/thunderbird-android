package net.thunderbird.app.composition.core.logging

import io.github.vinceglb.filekit.PlatformFile
import net.thunderbird.components.core.logging.DefaultLogger
import net.thunderbird.components.core.logging.LogLevel
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.components.core.logging.composite.CompositeLogSink
import net.thunderbird.components.core.logging.file.FileLogSink
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import net.thunderbird.core.file.DirectoryProvider
import net.thunderbird.core.logging.internal.DEFAULT_LOGGING_ENABLED
import net.thunderbird.core.logging.internal.SYNC_DEBUG_LOG
import net.thunderbird.core.logging.internal.coreLoggingModule
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val loggingCompositionModule = module {
    includes(coreLoggingModule)

    single<LogLevel> {
        if (get<PlatformConfigProvider>().isDebug) LogLevel.VERBOSE else LogLevel.DEBUG
    }
    single<Boolean>(named(DEFAULT_LOGGING_ENABLED)) { get<PlatformConfigProvider>().isDebug }

    single<CompositeLogSink>(named(SYNC_DEBUG_LOG)) {
        CompositeLogSink(
            logLevelProvider = get(),
            sinks = emptyList(),
        )
    }

    single<FileLogSink>(named(SYNC_DEBUG_LOG)) {
        FileLogSink(
            level = LogLevel.DEBUG,
            file = PlatformFile(
                PlatformFile(requireNotNull(get<DirectoryProvider>().getFilesDir().path)),
                "thunderbird-sync-debug",
            ),
        )
    }

    single<Logger>(named(SYNC_DEBUG_LOG)) {
        DefaultLogger(
            sink = get<CompositeLogSink>(named(SYNC_DEBUG_LOG)),
        )
    }
}
