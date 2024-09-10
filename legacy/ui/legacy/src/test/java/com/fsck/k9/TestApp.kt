package com.fsck.k9

import android.app.Application
import app.k9mail.feature.telemetry.telemetryModule
import app.k9mail.legacy.di.DI
import com.fsck.k9.preferences.InMemoryStoragePersister
import com.fsck.k9.preferences.StoragePersister
import org.koin.dsl.module

class TestApp : Application() {
    override fun onCreate() {
        Core.earlyInit()

        super.onCreate()
        DI.start(
            application = this,
            modules = coreModules + commonAppModules + uiModules + telemetryModule + testModule,
            allowOverride = true,
        )

        K9.init(this)
        Core.init(this)
    }
}

val testModule = module {
    single { AppConfig(emptyList()) }
    single<CoreResourceProvider> { TestCoreResourceProvider() }
    single<StoragePersister> { InMemoryStoragePersister() }
}
