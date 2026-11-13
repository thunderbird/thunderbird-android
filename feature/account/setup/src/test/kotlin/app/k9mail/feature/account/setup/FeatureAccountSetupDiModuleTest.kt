package app.k9mail.feature.account.setup

import android.app.Application
import android.content.Context
import app.k9mail.autodiscovery.api.AutoDiscovery
import app.k9mail.feature.migration.launcher.api.MigrationManager
import app.k9mail.feature.settings.import.SettingsImportExternalContract
import com.fsck.k9.mail.oauth.OAuth2TokenProviderFactory
import com.fsck.k9.mail.ssl.LocalKeyStore
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import net.thunderbird.core.common.oauth.OAuthConfigurationFactory
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.core.configstore.backend.ConfigBackendProvider
import net.thunderbird.core.featureflag.serialization.FlagRegistryOverrideSerializer
import net.thunderbird.core.file.FileSystemManager
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.mock

class FeatureAccountSetupDiModuleTest {

    private val externalDependencies = module {
        single<Application> { mock() }
        single<Context> { get<Application>() }
        single<Logger> { mock() }
        single<BrandNameProvider> { mock() }
        single<FileSystemManager> { mock() }
        single<ConfigBackendProvider> { mock() }
        single<FlagRegistryOverrideSerializer> { mock() }
        single<PlatformConfigProvider> { mock() }
        single<MigrationManager> { mock() }
        single<SettingsImportExternalContract.AccountActivator> { mock() }
        single<LegacyAccountManager> { mock() }
        single<OAuthConfigurationFactory> { mock() }
        single<OAuth2TokenProviderFactory> { mock() }
        single<TrustedSocketFactory> { mock() }
        single<LocalKeyStore> { mock() }
        single<AccountSetupExternalContract.AccountCreator> { mock() }
        single<AccountSetupExternalContract.AccountOwnerNameProvider> { mock() }
        single<List<AutoDiscovery>>(named("extraAutoDiscoveries")) { mock() }
        single<String>(named("ClientInfoAppName")) { "test" }
        single<String>(named("ClientInfoAppVersion")) { "1" }
    }

    @Test
    fun `should have a complete di graph`() {
        try {
            startKoin {
                modules(featureAccountSetupDiModule, externalDependencies)
            }
        } finally {
            stopKoin()
        }
    }
}
