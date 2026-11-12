package net.thunderbird.core.android.common

import android.content.Context
import net.thunderbird.core.common.oauth.OAuthConfigurationFactory
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

internal class CoreAndroidCommonDiModuleTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `should have a valid di module`() {
        coreAndroidCommonDiModule.verify(
            extraTypes = listOf(
                Context::class,
                OAuthConfigurationFactory::class,
            ),
        )
    }
}
