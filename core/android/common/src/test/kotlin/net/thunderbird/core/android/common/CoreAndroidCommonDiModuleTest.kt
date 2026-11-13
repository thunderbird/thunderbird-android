package net.thunderbird.core.android.common

import android.app.Application
import android.content.Context
import net.thunderbird.core.common.oauth.OAuthConfigurationFactory
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.mock

internal class CoreAndroidCommonDiModuleTest {

    private val externalDependencies = module {
        single<Application> { mock() }
        single<Context> { get<Application>() }
        single<OAuthConfigurationFactory> { mock() }
    }

    @Test
    fun `should have a complete di graph`() {
        try {
            startKoin {
                modules(coreAndroidCommonDiModule, externalDependencies)
            }
        } finally {
            stopKoin()
        }
    }
}
