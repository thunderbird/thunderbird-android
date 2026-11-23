package com.fsck.k9

import android.app.Application
import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.feature.launcher.FeatureLauncherExternalContract
import app.k9mail.feature.migration.launcher.api.MigrationManager
import com.fsck.k9.mail.Part
import com.fsck.k9.preferences.FilePrefixProvider
import net.thunderbird.backend.api.BackendFactory
import net.thunderbird.core.android.common.provider.NotificationIconResourceProvider
import net.thunderbird.core.android.common.startup.DatabaseUpgradeInterceptor
import net.thunderbird.core.common.oauth.OAuthConfigurationFactory
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.common.provider.AppVersionProvider
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.core.configstore.backend.ConfigBackendProvider
import net.thunderbird.core.featureflag.serialization.FlagRegistryOverrideSerializer
import net.thunderbird.core.file.DirectoryProvider
import net.thunderbird.core.file.FileManager
import net.thunderbird.core.file.FileSystemManager
import net.thunderbird.core.file.MimeTypeResolver
import net.thunderbird.core.logging.LogcatExporter
import net.thunderbird.core.logging.SyncDebugLogExporter
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.core.ui.theme.api.ThemeProvider
import net.thunderbird.feature.account.avatar.AvatarIcon
import net.thunderbird.feature.account.avatar.AvatarIconCatalog
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator
import net.thunderbird.feature.account.settings.api.AccountSettingsNavigation
import net.thunderbird.feature.account.settings.api.BackgroundAccountRemover
import net.thunderbird.feature.funding.api.FundingManager
import net.thunderbird.feature.funding.api.FundingNavigation
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater
import net.thunderbird.feature.mail.message.composer.html.MessageComposerHtmlSettingsProvider
import net.thunderbird.feature.mail.message.composer.signature.HtmlSignatureSanitizer
import net.thunderbird.feature.mail.message.export.MessageExporter
import net.thunderbird.feature.mail.message.export.MessageFileNameSuggester
import net.thunderbird.feature.mail.message.list.LocalDeleteOperationDecider
import net.thunderbird.feature.mail.message.list.LocalMessageUidPrefixProvider
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.ui.MessageListContract
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogFragmentFactory
import net.thunderbird.feature.mail.message.reader.api.domain.mapper.AttachmentViewInfoMapper
import net.thunderbird.feature.mail.message.reader.api.html.MessageReaderHtmlSettingsProvider
import net.thunderbird.feature.mail.message.reader.api.ui.MessageReaderViewContract
import net.thunderbird.feature.mail.message.reader.api.ui.bridge.MessageReaderBottomSheet
import net.thunderbird.feature.navigation.changelog.api.ChangelogConfigProvider
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract
import net.thunderbird.feature.notification.api.NotificationManager
import net.thunderbird.feature.notification.api.receiver.InAppNotificationStream
import net.thunderbird.feature.notification.api.ui.dialog.ErrorNotificationsDialogFragmentFactory
import net.thunderbird.feature.thundermail.navigation.ThundermailNavigation
import app.k9mail.feature.telemetry.featureTelemetryDiModule
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
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.mock

class TestApp : Application() {
    override fun onCreate() {
        Core.earlyInit()

        super.onCreate()

        Log.logger = logger
        startKoin {
            allowOverride(true)
            androidContext(this@TestApp)
            modules(legacyCoreDiModule, legacyCommonDiModule, legacyUiDiModule, featureTelemetryDiModule, testModule)
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
    single<OAuthConfigurationFactory> { OAuthConfigurationFactory { emptyMap() } }
    single<Map<String, BackendFactory>>(named("developmentBackends")) { emptyMap() }
    single(named("ClientInfoAppName")) { "TestApp" }
    single(named("ClientInfoAppVersion")) { "1.0" }

    // Koin compiler 1.2.3 validates deferred call sites from every dependency module (upstream issue #150).
    // These bindings satisfy that check only; the legacy UI test graph never resolves them.
    single<FileSystemManager> { notInTestGraph() }
    single<FileManager> { notInTestGraph() }
    single<DirectoryProvider> { notInTestGraph() }
    single<MimeTypeResolver> { notInTestGraph() }
    single<ConfigBackendProvider> { notInTestGraph() }
    single<FlagRegistryOverrideSerializer> { notInTestGraph() }
    single<ChangelogConfigProvider> { notInTestGraph() }
    single<MigrationManager> { notInTestGraph() }
    single<FilePrefixProvider> { notInTestGraph() }
    single<LocalDeleteOperationDecider> { notInTestGraph() }
    single<LocalMessageUidPrefixProvider> { notInTestGraph() }
    single<NotificationManager> { notInTestGraph() }
    single<NotificationIconResourceProvider> { notInTestGraph() }
    single<SpecialFolderUpdater.Factory> { notInTestGraph() }
    single<SyncDebugLogExporter> { notInTestGraph() }
    single<LogcatExporter> { notInTestGraph() }
    single<ThemeProvider> { notInTestGraph() }
    single<FeatureThemeProvider> { notInTestGraph() }
    single<AppNameProvider> { notInTestGraph() }
    single<AppVersionProvider> { notInTestGraph() }
    single<BrandNameProvider> { notInTestGraph() }
    single<DatabaseUpgradeInterceptor> { notInTestGraph() }
    single<FundingManager> { notInTestGraph() }
    single<FundingNavigation> { notInTestGraph() }
    single<AccountSettingsNavigation> { notInTestGraph() }
    single<BackgroundAccountRemover> { notInTestGraph() }
    single<ThundermailNavigation> { notInTestGraph() }
    single<FeatureLauncherExternalContract.MessageListLauncher> { notInTestGraph() }
    single<NavigationDrawerExternalContract.DrawerConfigLoader> { notInTestGraph() }
    single<NavigationDrawerExternalContract.DrawerConfigWriter> { notInTestGraph() }
    single<InAppNotificationStream> { notInTestGraph() }
    single<ErrorNotificationsDialogFragmentFactory> { notInTestGraph() }
    single<AvatarIconCatalog<AvatarIcon<ImageVector>>> { notInTestGraph() }
    single<AvatarMonogramCreator> { notInTestGraph() }
    single<MessageListContract.ViewModel> { notInTestGraph() }
    single<MessageListContract.MessageListScreenRenderer> { notInTestGraph() }
    single<SetupArchiveFolderDialogFragmentFactory> { notInTestGraph() }
    single<DomainContract.UseCase.BuildSwipeActions> { notInTestGraph() }
    single<MessageReaderHtmlSettingsProvider> { notInTestGraph() }
    single<MessageComposerHtmlSettingsProvider> { notInTestGraph() }
    single<HtmlSignatureSanitizer> { notInTestGraph() }
    single<AttachmentViewInfoMapper<Part>> { notInTestGraph() }
    single<MessageReaderViewContract.ViewModel<Part>> { notInTestGraph() }
    single<MessageReaderBottomSheet> { notInTestGraph() }
    single<MessageExporter> { notInTestGraph() }
    single<MessageFileNameSuggester> { notInTestGraph() }
}

private fun notInTestGraph(): Nothing = error("Not part of the legacy UI test graph")

class FakePlatformConfigProvider : PlatformConfigProvider {
    override val isDebug: Boolean
        get() = true
}
