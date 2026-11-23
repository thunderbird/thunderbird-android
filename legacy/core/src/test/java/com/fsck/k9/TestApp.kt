package com.fsck.k9

import android.app.Application
import androidx.work.WorkManager
import app.k9mail.feature.telemetry.featureTelemetryDiModule
import com.fsck.k9.autocrypt.AutocryptStringProvider
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.controller.ControllerExtension
import com.fsck.k9.crypto.EncryptionExtractor
import com.fsck.k9.mail.oauth.OAuth2TokenProviderFactory
import com.fsck.k9.notification.NotificationActionCreator
import com.fsck.k9.notification.NotificationResourceProvider
import com.fsck.k9.notification.NotificationStrategy
import com.fsck.k9.notification.TestNotificationIconResourceProvider
import com.fsck.k9.preferences.FilePrefixProvider
import com.fsck.k9.storage.legacyStorageDiModule
import net.thunderbird.components.core.logging.LogLevel
import net.thunderbird.components.core.logging.LogLevelProvider
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.components.core.logging.LoggingControl
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.android.common.provider.NotificationIconResourceProvider
import net.thunderbird.core.android.preferences.TestStoragePersister
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import net.thunderbird.core.common.inject.factoryListOf
import net.thunderbird.core.common.oauth.OAuthConfigurationFactory
import net.thunderbird.core.configstore.backend.ConfigBackendProvider
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.featureflag.serialization.FlagRegistryOverrideSerializer
import net.thunderbird.core.file.FileSystemManager
import net.thunderbird.core.logging.DebugLogConfigurator
import net.thunderbird.core.logging.SyncDebugLogExporter
import net.thunderbird.core.preference.storage.StoragePersister
import net.thunderbird.feature.account.avatar.AvatarImageRepository
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater
import net.thunderbird.feature.mail.message.list.LocalDeleteOperationDecider
import net.thunderbird.feature.mail.message.list.LocalMessageUidPrefixProvider
import net.thunderbird.feature.mail.message.list.ui.MessageListContract
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import net.thunderbird.feature.mail.message.reader.api.css.CssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.css.CssVariableNameProvider
import net.thunderbird.feature.notification.api.NotificationManager
import net.thunderbird.feature.notification.api.receiver.InAppNotificationStream
import net.thunderbird.legacy.core.FakeAccountDefaultsProvider
import net.thunderbird.legacy.core.StubLocalDeleteOperationDecider
import net.thunderbird.legacy.core.mailstore.folder.FakeLocalMessageUidPrefixProvider
import net.thunderbird.legacy.core.mailstore.folder.FakeOutboxFolderManager
import net.thunderbird.legacy.logging.Log
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TestApp : Application() {
    override fun onCreate() {
        Core.earlyInit()

        super.onCreate()

        Log.logger = logger
        startKoin {
            allowOverride(true)
            androidContext(this@TestApp)
            modules(legacyCoreDiModule, legacyStorageDiModule, featureTelemetryDiModule, testModule)
        }

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
    single<StoragePersister> { TestStoragePersister(logger = get()) }
    single { mock<BackendManager>() }
    single { mock<NotificationResourceProvider>() }
    single { mock<NotificationActionCreator>() }
    single { mock<NotificationStrategy>() }
    single(named("controllerExtensions")) { emptyList<ControllerExtension>() }
    single<AccountDefaultsProvider> { FakeAccountDefaultsProvider() }
    single { mock<WorkManager>() }
    single<OutboxFolderManager> { FakeOutboxFolderManager() }
    single<LegacyAccountManager> { mock() }
    single<AvatarImageRepository> { mock() }
    single<NotificationIconResourceProvider> { TestNotificationIconResourceProvider() }
    single<PlatformConfigProvider> { FakePlatformConfigProvider() }
    single<OAuthConfigurationFactory> { OAuthConfigurationFactory { emptyMap() } }
    single<FeatureFlagProvider> { FeatureFlagProvider { FeatureFlagResult.Disabled } }
    single<CssVariableNameProvider> { mock() }
    single<CssClassNameProvider> {
        mock {
            whenever(it.plainTextMessagePreClassName).doReturn("k9mail")
        }
    }
    factoryListOf<CssStyleProvider>()
    single<NotificationResourceProvider> { mock() }
    single<LocalMessageUidPrefixProvider> { FakeLocalMessageUidPrefixProvider() }
    single<AutocryptStringProvider> { mock() }
    single<LocalDeleteOperationDecider> { StubLocalDeleteOperationDecider() }
    single<NotificationManager> { mock() }
    single<SyncDebugLogExporter> { mock() }
    single<SpecialFolderUpdater.Factory> { mock() }
    single<FilePrefixProvider> {
        object : FilePrefixProvider {
            override val filePrefix = "test"
        }
    }
    single<OAuth2TokenProviderFactory> { mock() }
    single(named("ClientInfoAppName")) { "TestApp" }
    single(named("ClientInfoAppVersion")) { "1.0" }

    // Koin 1.2.3 validates deferred call sites from unused dependency wiring (upstream issue #150).
    // These bindings must never be resolved by the legacy test graph.
    single<FileSystemManager> { error("Remote feature-flag file access is not part of this test graph") }
    single<ConfigBackendProvider> { error("Remote feature-flag configuration is not part of this test graph") }
    single<FlagRegistryOverrideSerializer> {
        error("Feature-flag override serialization is not part of this test graph")
    }
    single<MessageListContract.ViewModel> { error("Compose message-list rendering is not part of this test graph") }
    single<InAppNotificationStream> { error("Compose notification rendering is not part of this test graph") }
}

class FakePlatformConfigProvider : PlatformConfigProvider {
    override val isDebug: Boolean
        get() = true
}
