package net.thunderbird.feature.funding

import android.content.Context
import kotlin.test.Test
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.core.android.common.activity.ActivityProvider
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.configstore.backend.ConfigBackendProvider
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.funding.api.FundingSettings
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.mock

class FeatureFundingDiModuleTest {

    private val externalDependencies = module {
        single<Logger> { mock() }
        single<AppNameProvider> { mock() }
        single<FeatureThemeProvider> { mock() }
        single<ActivityProvider> { mock() }
        single<FundingSettings> { mock() }
        single<Context> { mock() }
        single<Clock> { mock() }
        single<ConfigBackendProvider> { mock() }
        single<CoroutineScope>(named("ConfigStoreScope")) { mock() }
    }

    @Test
    fun `should have a complete di graph`() {
        try {
            startKoin {
                modules(featureFundingDiModule, externalDependencies)
            }
        } finally {
            stopKoin()
        }
    }
}
