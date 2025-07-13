package com.fsck.k9.job

import android.content.Context
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import kotlin.time.ExperimentalTime
import net.thunderbird.core.logging.Logger
import org.koin.core.qualifier.named
import org.koin.dsl.module

val jobModule = module {
    single { WorkManagerConfigurationProvider(workerFactory = get()) }
    single<WorkerFactory> { K9WorkerFactory() }
    single { WorkManager.getInstance(get<Context>()) }
    single {
        K9JobManager(
            workManager = get(),
            accountManager = get(),
            mailSyncWorkerManager = get(),
        )
    }
    factory {
        @OptIn(ExperimentalTime::class)
        MailSyncWorkerManager(
            workManager = get(),
            clock = get(),
            syncDebugLogger = get<Logger>(named("syncDebug")),
            generalSettingsManager = get(),
        )
    }
    factory { (parameters: WorkerParameters) ->
        MailSyncWorker(
            messagingController = get(),
            preferences = get(),
            context = get(),
            generalSettingsManager = get(),
            parameters = parameters,
        )
    }
}
