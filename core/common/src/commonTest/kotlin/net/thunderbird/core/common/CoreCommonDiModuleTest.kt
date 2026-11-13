package net.thunderbird.core.common

import net.thunderbird.core.common.oauth.OAuthConfigurationFactory
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

internal class CoreCommonDiModuleTest {

    private val externalDependencies = module {
        single<OAuthConfigurationFactory> { OAuthConfigurationFactory { emptyMap() } }
    }

    @Test
    fun `should have a complete di graph`() {
        try {
            startKoin {
                modules(coreCommonDiModule, externalDependencies)
            }
        } finally {
            stopKoin()
        }
    }
}
