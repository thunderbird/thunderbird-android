package app.k9mail.core.common

import app.k9mail.core.common.oauth.OAuthConfigurationFactory
import org.junit.Test
import org.koin.test.verify.verify

internal class CoreCommonModuleKtTest {

    @Test
    fun `should have a valid di module`() {
        coreCommonModule.verify(
            extraTypes = listOf(
                OAuthConfigurationFactory::class,
            ),
        )
    }
}
