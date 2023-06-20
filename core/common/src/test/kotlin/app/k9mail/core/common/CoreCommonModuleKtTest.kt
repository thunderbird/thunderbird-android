package app.k9mail.core.common

import app.k9mail.core.common.oauth.OAuthConfigurationFactory
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.check.checkModules
import org.koin.test.verify.verify

@OptIn(KoinExperimentalAPI::class)
internal class CoreCommonModuleKtTest {

    private val externalModule = module {
        single<OAuthConfigurationFactory> {
            OAuthConfigurationFactory { emptyMap() }
        }
    }

    @Test
    fun `should have a valid di module`() {
        coreCommonModule.verify(
            extraTypes = listOf(
                OAuthConfigurationFactory::class,
            ),
        )

        koinApplication {
            modules(externalModule, coreCommonModule)
            checkModules()
        }
    }
}
