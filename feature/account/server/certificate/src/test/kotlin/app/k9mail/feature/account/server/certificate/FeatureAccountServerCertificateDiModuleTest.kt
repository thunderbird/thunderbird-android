package app.k9mail.feature.account.server.certificate

import com.fsck.k9.mail.ssl.LocalKeyStore
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.mock

class FeatureAccountServerCertificateDiModuleTest {

    private val externalDependencies = module {
        single<LocalKeyStore> { mock() }
    }

    @Test
    fun `should have a complete di graph`() {
        try {
            startKoin {
                modules(featureAccountServerCertificateDiModule, externalDependencies)
            }
        } finally {
            stopKoin()
        }
    }
}
