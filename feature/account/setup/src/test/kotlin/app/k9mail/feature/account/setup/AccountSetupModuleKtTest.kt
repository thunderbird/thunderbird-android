package app.k9mail.feature.account.setup

import app.k9mail.core.common.oauth.OAuthConfigurationFactory
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import app.k9mail.feature.account.setup.ui.AccountSetupContract
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import okhttp3.OkHttpClient
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
    }

    @Test
    fun `should have a valid di module`() {
        featureAccountSetupModule.verify(
            extraTypes = listOf(
                AccountSetupContract.State::class,
                AccountAutoDiscoveryContract.State::class,
                AccountValidationContract.State::class,
                AccountIncomingConfigContract.State::class,
                AccountOutgoingConfigContract.State::class,
                AccountOptionsContract.State::class,
            ),
        )

        koinApplication {
            modules(externalModule, featureAccountSetupModule)
            androidContext(RuntimeEnvironment.getApplication())
            checkModules()
        }
    }
}
