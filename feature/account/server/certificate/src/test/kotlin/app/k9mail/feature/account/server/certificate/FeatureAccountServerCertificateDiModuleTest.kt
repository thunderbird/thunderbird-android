package app.k9mail.feature.account.server.certificate

import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.KoinTest
import org.koin.test.verify.verify

class FeatureAccountServerCertificateDiModuleTest : KoinTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `should have a valid di module`() {
        featureAccountServerCertificateDiModule.verify(
            extraTypes = listOf(
                ServerCertificateErrorContract.State::class,
            ),
        )
    }
}
