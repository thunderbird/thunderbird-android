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
import com.fsck.k9.legacyCoreModule
import com.fsck.k9.preferences.K9StoragePersister
import com.fsck.k9.storage.messages.FakeLocalMessageUidPrefixProvider
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import net.thunderbird.components.core.logging.LogLevel
import net.thunderbird.components.core.logging.LogLevelProvider
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.components.core.logging.LoggingControl
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.core.logging.DebugLogConfigurator
import net.thunderbird.core.preference.storage.StoragePersister
import net.thunderbird.feature.mail.message.list.LocalMessageUidPrefixProvider
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
            modules = legacyCoreModule + storageModule + telemetryModule + testModule,
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
    single<AppConfig> { DefaultAppConfig(emptyList()) }
    single { mock<CoreResourceProvider>() }
    single { mock<EncryptionExtractor>() }
    single<StoragePersister> { K9StoragePersister(get(), get()) }
    single { mock<BackendManager>() }
    single<AccountDefaultsProvider> { mock<AccountDefaultsProvider>() }
    single<LegacyAccountManager> { mock() }
    single<NotificationIconResourceProvider> {
        object : NotificationIconResourceProvider {
            override val pushNotificationIcon: Int = 0
        }
    }
    single<PlatformConfigProvider> { FakePlatformConfigProvider() }
    single<LocalMessageUidPrefixProvider> { FakeLocalMessageUidPrefixProvider() }
}

class FakePlatformConfigProvider : PlatformConfigProvider {
    override val isDebug: Boolean
        get() = true
}
