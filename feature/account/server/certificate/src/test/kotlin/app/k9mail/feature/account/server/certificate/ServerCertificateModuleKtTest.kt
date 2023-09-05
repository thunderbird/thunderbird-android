package app.k9mail.feature.account.server.certificate

import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract
import com.fsck.k9.mail.ssl.LocalKeyStore
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.check.checkModules
import org.koin.test.verify.verify
import org.mockito.Mockito

class ServerCertificateModuleKtTest : KoinTest {

    private val externalModule: Module = module {
        single<LocalKeyStore> { Mockito.mock() }
    }

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `should have a valid di module`() {
        featureAccountServerCertificateModule.verify(
            extraTypes = listOf(
                ServerCertificateErrorContract.State::class,
            ),
        )

        koinApplication {
            modules(externalModule, featureAccountServerCertificateModule)
            checkModules()
        }
    }
}
