package net.thunderbird.core.common

import net.thunderbird.core.common.oauth.OAuthConfigurationFactory
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

internal class CoreCommonModuleKtTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `should have a valid di module`() {
        coreCommonModule.verify(
            extraTypes = listOf(
                OAuthConfigurationFactory::class,
            ),
        )
    }
}
