package com.fsck.k9.storage

import android.app.Application
import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import app.k9mail.feature.telemetry.telemetryModule
import app.k9mail.legacy.di.DI
import com.fsck.k9.AppConfig
import com.fsck.k9.Core
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.DefaultAppConfig
import com.fsck.k9.K9
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.crypto.EncryptionExtractor
import com.fsck.k9.legacyCoreModules
import com.fsck.k9.preferences.K9StoragePersister
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.LegacyAccountManager
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
import org.mockito.kotlin.mock

class TestApp : Application() {
    override fun onCreate() {
        Core.earlyInit()

        super.onCreate()

        Log.logger = logger
        DI.start(
            application = this,
            modules = legacyCoreModules + storageModule + telemetryModule + testModule,
        )

        K9.init(this)
        Core.init(this)
    }

    companion object {
        val logger: Logger = TestLogger()
        val sinkManager: CompositeLogSinkManager = mock<CompositeLogSinkManager>()
        val fileSink: FileLogSink = mock<FileLogSink>()

        val compositeSink: CompositeLogSink = CompositeLogSink(
            logLevelProvider = { LogLevel.DEBUG },
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
    single<AppConfig> { DefaultAppConfig(emptyList()) }
    single { mock<CoreResourceProvider>() }
    single { mock<EncryptionExtractor>() }
    single<StoragePersister> { K9StoragePersister(get(), get()) }
    single { mock<BackendManager>() }
    single<AccountDefaultsProvider> { mock<AccountDefaultsProvider>() }
    single<FeatureFlagProvider> {
        InMemoryFeatureFlagProvider(
            featureFlagFactory = {
                emptyList<FeatureFlag>()
            },
        )
    }
    single<LegacyAccountManager> { mock() }
    single<NotificationIconResourceProvider> {
        object : NotificationIconResourceProvider {
            override val pushNotificationIcon: Int = 0
        }
    }
}
