package com.fsck.k9

import android.app.Application
import app.k9mail.feature.telemetry.telemetryModule
import app.k9mail.legacy.di.DI
import com.fsck.k9.contacts.ContactPictureLoader
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.preferences.TestStoragePersister
import net.thunderbird.core.featureflag.FeatureFlag
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.InMemoryFeatureFlagProvider
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogLevelManager
import net.thunderbird.core.logging.LogLevelProvider
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.composite.CompositeLogSink
import net.thunderbird.core.logging.composite.CompositeLogSinkManager
import net.thunderbird.core.logging.file.FileLogSink
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogLevelManager
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.preference.storage.StoragePersister
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mockito.Mockito.mock

class TestApp : Application() {
    override fun onCreate() {
        Core.earlyInit()

        super.onCreate()

        Log.logger = logger
        DI.start(
            application = this,
            modules = legacyCoreModules + legacyCommonAppModules + legacyUiModules + telemetryModule + testModule,
            allowOverride = true,
        )

        K9.init(this)
        Core.init(this)
    }

    companion object {
        val logger: Logger = TestLogger()
        val sinkManager: CompositeLogSinkManager = org.mockito.kotlin.mock<CompositeLogSinkManager>()
        val fileSink: FileLogSink = org.mockito.kotlin.mock<FileLogSink>()

        val compositeSink: CompositeLogSink = CompositeLogSink(
            level = LogLevel.DEBUG,
            manager = sinkManager,
            sinks = listOf(fileSink),
        )
    }
}

val testModule = module {
    single<Logger> { TestApp.logger }
    single<LogLevelManager> { TestLogLevelManager() }.bind<LogLevelProvider>()
    single(named("syncDebug")) { TestApp.logger }
    single(named("syncDebug")) { TestApp.compositeSink }
    single(named("syncDebug")) { TestApp.fileSink }
    single(named("syncDebug")) { TestApp.sinkManager }
    single<AppConfig> { DefaultAppConfig(componentsToDisable = emptyList()) }
    single<CoreResourceProvider> { TestCoreResourceProvider() }
    single<StoragePersister> {
        TestStoragePersister(
            logger = get(),
        )
    }
    single<AccountDefaultsProvider> { mock<AccountDefaultsProvider>() }
    single<FeatureFlagProvider> {
        InMemoryFeatureFlagProvider(
            featureFlagFactory = {
                emptyList<FeatureFlag>()
            },
        )
    }

    single<ContactPictureLoader> { mock() }
}
