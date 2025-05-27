package com.fsck.k9.storage

import android.app.Application
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
import net.thunderbird.core.featureflag.FeatureFlag
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.InMemoryFeatureFlagProvider
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.preference.storage.StoragePersister
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
    }
}

val testModule = module {
    single<Logger> { TestApp.logger }
    single<AppConfig> { DefaultAppConfig(emptyList()) }
    single { mock<CoreResourceProvider>() }
    single { mock<EncryptionExtractor>() }
    single<StoragePersister> { K9StoragePersister(get()) }
    single { mock<BackendManager>() }
    single<AccountDefaultsProvider> { mock<AccountDefaultsProvider>() }
    single<FeatureFlagProvider> {
        InMemoryFeatureFlagProvider(
            featureFlagFactory = {
                emptyList<FeatureFlag>()
            },
        )
    }
}
