package net.thunderbird.feature.changelog.internal

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.core.common.coreCommonDiModule
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.storage.StorageProvider
import net.thunderbird.feature.navigation.changelog.api.ChangelogConfigProvider
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.mock

class FeatureChangelogDiModuleTest {

    private val externalDependencies = module {
        single<CoroutineScope>(named("AppCoroutineScope")) { mock() }
        single<Context> { mock() }
        single<ChangelogConfigProvider> { mock() }
        single<Logger> { mock() }
        single<GeneralSettingsManager> { mock() }
        single<StorageProvider> { mock() }
    }

    @Test
    fun `should have a complete di graph`() {
        try {
            startKoin {
                modules(featureChangelogDiModule, externalDependencies)
            }
        } finally {
            stopKoin()
        }
    }
}
