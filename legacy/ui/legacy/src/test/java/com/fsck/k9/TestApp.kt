package com.fsck.k9

import android.app.Application
import app.k9mail.feature.telemetry.telemetryModule
import app.k9mail.legacy.di.DI
import com.fsck.k9.contacts.ContactPictureLoader
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.android.preferences.TestStoragePersister
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import net.thunderbird.core.common.inject.factoryListOf
import net.thunderbird.components.core.logging.LogLevel
import net.thunderbird.components.core.logging.LogLevelProvider
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.components.core.logging.LoggingControl
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.core.logging.DebugLogConfigurator
import net.thunderbird.core.preference.storage.StoragePersister
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import net.thunderbird.feature.mail.message.reader.api.css.CssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.css.CssVariableNameProvider
import net.thunderbird.legacy.logging.Log
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.mock

class TestApp : Application() {
    override fun onCreate() {
        Core.earlyInit()

        super.onCreate()

        Log.logger = logger
        DI.start(
            application = this,
            modules = legacyCoreModule + legacyCommonAppModules + legacyUiModules + telemetryModule + testModule,
            allowOverride = true,
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
    single<LogLevelProvider> { LogLevelProvider { LogLevel.DEBUG } }
    single<Logger>(named("syncDebug")) { TestApp.logger }
    single<DebugLogConfigurator> { mock() }
    single<LoggingControl> { mock() }
    single<AppConfig> { DefaultAppConfig(componentsToDisable = emptyList()) }
    single<CoreResourceProvider> { TestCoreResourceProvider() }
    single<StoragePersister> {
        TestStoragePersister(
            logger = get(),
        )
    }
    single<AccountDefaultsProvider> { mock<AccountDefaultsProvider>() }
    single<ContactPictureLoader> { mock() }
    single<LegacyAccountManager> { mock() }
    single<PlatformConfigProvider> { FakePlatformConfigProvider() }
    single<CssVariableNameProvider> { mock() }
    single<CssClassNameProvider> { mock() }
    factoryListOf<CssStyleProvider>()
}

class FakePlatformConfigProvider : PlatformConfigProvider {
    override val isDebug: Boolean
        get() = true
}
