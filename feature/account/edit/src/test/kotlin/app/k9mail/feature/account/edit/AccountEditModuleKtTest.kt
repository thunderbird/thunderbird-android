package app.k9mail.feature.account.edit

import android.content.Context
import app.k9mail.core.common.oauth.OAuthConfigurationFactory
import app.k9mail.feature.account.common.AccountCommonExternalContract
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsContract
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.oauth.OAuth2TokenProviderFactory
import com.fsck.k9.mail.ssl.LocalKeyStore
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import java.net.Socket
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.check.checkModules
import org.koin.test.verify.verify
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(KoinExperimentalAPI::class)
@RunWith(RobolectricTestRunner::class)
class AccountEditModuleKtTest : KoinTest {

    private val externalModule: Module = module {
        single<AccountCommonExternalContract.AccountStateLoader> { Mockito.mock() }
        single<LocalKeyStore> { Mockito.mock() }
        single<TrustedSocketFactory> {
            object : TrustedSocketFactory {
                override fun createSocket(
                    socket: Socket?,
                    host: String,
                    port: Int,
                    clientCertificateAlias: String?,
                ): Socket {
                    return Mockito.mock()
                }
            }
        }
        single<OAuthConfigurationFactory> { OAuthConfigurationFactory { emptyMap() } }
        single<OAuth2TokenProviderFactory> {
            OAuth2TokenProviderFactory { _ ->
                object : OAuth2TokenProvider {
                    override val primaryEmail: String? get() = TODO()
                    override fun getToken(timeoutMillis: Long) = TODO()
                    override fun invalidateToken() = TODO()
                }
            }
        }
        single<AccountEditExternalContract.AccountServerSettingsUpdater> { Mockito.mock() }
    }

    @Test
    fun `should have a valid di module`() {
        featureAccountEditModule.verify(
            extraTypes = listOf(
                Context::class,
                AccountState::class,
                Class.forName("net.openid.appauth.AppAuthConfiguration").kotlin,
                ServerValidationContract.State::class,
                ServerCertificateErrorContract.State::class,
                IncomingServerSettingsContract.State::class,
                OutgoingServerSettingsContract.State::class,
                SaveServerSettingsContract.State::class,
                AccountEditExternalContract.AccountServerSettingsUpdater::class,
                InteractionMode::class,
            ),
        )

        koinApplication {
            modules(externalModule, featureAccountEditModule)
            androidContext(RuntimeEnvironment.getApplication())
            checkModules()
        }
    }
}
