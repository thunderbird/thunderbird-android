package app.k9mail.feature.account.server.validation

import android.content.Context
import app.k9mail.core.common.oauth.OAuthConfigurationFactory
import app.k9mail.feature.account.common.AccountCommonExternalContract
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.server.certificate.domain.ServerCertificateDomainContract
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.oauth.OAuth2TokenProviderFactory
import com.fsck.k9.mail.ssl.LocalKeyStore
import com.fsck.k9.mail.ssl.TrustedSocketFactory
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

@RunWith(RobolectricTestRunner::class)
class ServerValidationModuleKtTest : KoinTest {

    private val externalModule: Module = module {
        single<TrustedSocketFactory> {
            TrustedSocketFactory { _, _, _, _ -> null }
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
        factory<AccountCommonExternalContract.AccountStateLoader> { mock() }
        single(named("ClientInfoAppName")) { "App Name" }
        single(named("ClientInfoAppVersion")) { "App Version" }
    }

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `should have a valid di module`() {
        featureAccountServerValidationModule.verify(
            extraTypes = listOf(
                ServerValidationContract.State::class,
                AccountDomainContract.AccountStateRepository::class,
                AccountCommonExternalContract.AccountStateLoader::class,
                ServerCertificateDomainContract.ServerCertificateErrorRepository::class,
                ServerCertificateErrorContract.State::class,
                AccountState::class,
                Context::class,
                Boolean::class,
                Class.forName("net.openid.appauth.AppAuthConfiguration").kotlin,
            ),
        )

        koinApplication {
            modules(externalModule, featureAccountServerValidationModule)
            androidContext(RuntimeEnvironment.getApplication())
            checkModules()
        }
    }
}
