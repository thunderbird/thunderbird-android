package com.fsck.k9

import android.app.Application
import androidx.work.WorkManager
import app.k9mail.core.featureflag.FeatureFlag
import app.k9mail.core.featureflag.FeatureFlagProvider
import app.k9mail.core.featureflag.InMemoryFeatureFlagProvider
import app.k9mail.feature.telemetry.telemetryModule
import app.k9mail.legacy.di.DI
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.controller.ControllerExtension
import com.fsck.k9.crypto.EncryptionExtractor
import com.fsck.k9.notification.NotificationActionCreator
import com.fsck.k9.notification.NotificationResourceProvider
import com.fsck.k9.notification.NotificationStrategy
import com.fsck.k9.preferences.StoragePersister
import com.fsck.k9.storage.storageModule
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.preferences.InMemoryStoragePersister
import net.thunderbird.legacy.core.FakeAccountDefaultsProvider
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.mock

class TestApp : Application() {
    override fun onCreate() {
        Core.earlyInit()

        super.onCreate()
        DI.start(
            application = this,
            modules = legacyCoreModules + storageModule + telemetryModule + testModule,
            allowOverride = true,
        )

        K9.init(this)
        Core.init(this)
    }
}

val testModule = module {
    single { AppConfig(emptyList()) }
    single { mock<CoreResourceProvider>() }
    single { mock<EncryptionExtractor>() }
    single<StoragePersister> { InMemoryStoragePersister() }
    single { mock<BackendManager>() }
    single { mock<NotificationResourceProvider>() }
    single { mock<NotificationActionCreator>() }
    single { mock<NotificationStrategy>() }
    single(named("controllerExtensions")) { emptyList<ControllerExtension>() }
    single<AccountDefaultsProvider> { FakeAccountDefaultsProvider() }
    single { mock<WorkManager>() }
    single<FeatureFlagProvider> {
        InMemoryFeatureFlagProvider(
            featureFlagFactory = {
                emptyList<FeatureFlag>()
            },
        )
    }
}
