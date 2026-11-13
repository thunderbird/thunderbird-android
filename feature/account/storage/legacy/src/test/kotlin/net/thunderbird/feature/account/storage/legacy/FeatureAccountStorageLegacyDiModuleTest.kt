package net.thunderbird.feature.account.storage.legacy

import kotlin.test.Test
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.feature.notification.api.receiver.InAppNotificationStream
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.mock

class FeatureAccountStorageLegacyDiModuleTest {

    private val externalDependencies = module {
        single<Logger> { mock() }
        single<InAppNotificationStream> { mock() }
    }

    @Test
    fun `should have a complete di graph`() {
        try {
            startKoin {
                modules(featureAccountStorageLegacyDiModule, externalDependencies)
            }
        } finally {
            stopKoin()
        }
    }
}
