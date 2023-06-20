package app.k9mail.core.common

import app.k9mail.core.common.oauth.OAuthProvider
import app.k9mail.core.common.oauth.OAuthProviderSettings
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.check.checkModules
import org.koin.test.verify.verify

@OptIn(KoinExperimentalAPI::class)
internal class CoreCommonModuleKtTest {

    private val externalModule = module {
        single<OAuthProviderSettings> {
            OAuthProviderSettings(
                applicationId = "test",
                clientIds = OAuthProvider.values().associateWith { "testClientId" },
                redirectUriIds = OAuthProvider.values().associateWith { "testRedirectUriId" },
            )
        }
    }

    @Test
    fun `should have a valid di module`() {
        coreCommonModule.verify(
            extraTypes = listOf(
                OAuthProviderSettings::class,
            ),
        )

        koinApplication {
            modules(externalModule, coreCommonModule)
            checkModules()
        }
    }
}
