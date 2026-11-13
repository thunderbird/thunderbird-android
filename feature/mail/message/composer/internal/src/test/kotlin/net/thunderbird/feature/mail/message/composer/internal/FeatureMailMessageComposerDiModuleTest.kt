package net.thunderbird.feature.mail.message.composer.internal

import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.core.ui.theme.api.ThemeManager
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.mock

class FeatureMailMessageComposerDiModuleTest {

    private val externalDependencies = module {
        single<ThemeManager> { mock() }
        single<FeatureThemeProvider> { mock() }
    }

    @Test
    fun `should have a complete di graph`() {
        try {
            startKoin {
                modules(featureMailMessageComposerDiModule, externalDependencies)
            }
        } finally {
            stopKoin()
        }
    }
}
