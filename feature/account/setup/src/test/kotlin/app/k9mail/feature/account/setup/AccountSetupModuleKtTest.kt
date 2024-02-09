package app.k9mail.feature.account.setup

import android.content.Context
import app.k9mail.autodiscovery.api.AutoDiscovery
import app.k9mail.core.common.oauth.OAuthConfigurationFactory
import app.k9mail.feature.account.common.AccountCommonExternalContract
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract
import com.fsck.k9.mail.oauth.AuthStateStorage
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.oauth.OAuth2TokenProviderFactory
import com.fsck.k9.mail.ssl.LocalKeyStore
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import okhttp3.OkHttpClient
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.check.checkModules
import org.koin.test.verify.verify
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(KoinExperimentalAPI::class)
@RunWith(RobolectricTestRunner::class)
class AccountSetupModuleKtTest : KoinTest {

    private val externalModule: Module = module {
        single<OkHttpClient> { OkHttpClient() }
        single<TrustedSocketFactory> {
            TrustedSocketFactory { _, _, _, _ -> null }
        }
        single<AccountCreator> {
            AccountCreator { _ -> AccountCreatorResult.Success("accountUuid") }
        }
        single<OAuthConfigurationFactory> { OAuthConfigurationFactory { emptyMap() } }
        single<OAuth2TokenProviderFactory> {
            OAuth2TokenProviderFactory { _ ->
                object : OAuth2TokenProvider {
                    override fun getToken(timeoutMillis: Long) = TODO()
                    override fun invalidateToken() = TODO()
                }
            }
        }
        single<LocalKeyStore> { mock() }
        single<AccountCommonExternalContract.AccountStateLoader> { mock() }
        factory<AccountSetupExternalContract.AccountOwnerNameProvider> { mock() }
        single<List<AutoDiscovery>>(named("extraAutoDiscoveries")) { emptyList() }
    }

    @Test
    fun `should have a valid di module`() {
        featureAccountSetupModule.verify(
            extraTypes = listOf(
                AccountCommonExternalContract.AccountStateLoader::class,
                AccountAutoDiscoveryContract.State::class,
                AccountOAuthContract.State::class,
                ServerValidationContract.State::class,
                IncomingServerSettingsContract.State::class,
                OutgoingServerSettingsContract.State::class,
                DisplayOptionsContract.State::class,
                SyncOptionsContract.State::class,
                AccountState::class,
                ServerCertificateErrorContract.State::class,
                AuthStateStorage::class,
                Context::class,
                Boolean::class,
                Class.forName("net.openid.appauth.AppAuthConfiguration").kotlin,
                InteractionMode::class,
                SpecialFoldersContract.State::class,
                CreateAccountContract.State::class,
                AccountSetupExternalContract.AccountOwnerNameProvider::class,
            ),
        )

        koinApplication {
            modules(externalModule, featureAccountSetupModule)
            androidContext(RuntimeEnvironment.getApplication())
            checkModules()
        }
    }
}
