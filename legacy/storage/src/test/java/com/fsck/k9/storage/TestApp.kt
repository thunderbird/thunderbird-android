package com.fsck.k9.storage

import android.app.Application
import app.k9mail.core.featureflag.FeatureFlag
import app.k9mail.core.featureflag.FeatureFlagProvider
import app.k9mail.core.featureflag.InMemoryFeatureFlagProvider
import app.k9mail.feature.telemetry.telemetryModule
import app.k9mail.legacy.account.AccountDefaultsProvider
import app.k9mail.legacy.di.DI
import com.fsck.k9.AppConfig
import com.fsck.k9.Core
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.K9
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.coreModules
import com.fsck.k9.crypto.EncryptionExtractor
import com.fsck.k9.preferences.K9StoragePersister
import com.fsck.k9.preferences.StoragePersister
import org.koin.dsl.module
import org.mockito.kotlin.mock

class TestApp : Application() {
    override fun onCreate() {
        Core.earlyInit()

        super.onCreate()
        DI.start(this, coreModules + storageModule + telemetryModule + testModule)

        K9.init(this)
        Core.init(this)
    }
}

val testModule = module {
    single { AppConfig(emptyList()) }
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
