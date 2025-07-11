package app.k9mail.feature.account.server.validation

import android.content.Context
import app.k9mail.feature.account.common.AccountCommonExternalContract
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.server.certificate.domain.ServerCertificateDomainContract
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.verify.verify

class ServerValidationModuleKtTest : KoinTest {

    @Test
    fun `should have a valid di module`() {
        featureAccountServerValidationModule.verify(
            extraTypes = listOf(
                ServerValidationContract.State::class,
                AccountDomainContract.AccountStateRepository::class,
                AccountCommonExternalContract.AccountStateLoader::class,
                ServerCertificateDomainContract.ServerCertificateErrorRepository::class,
                ServerCertificateErrorContract.State::class,
                AccountState::class,
                Context::class,
                Boolean::class,
                Class.forName("net.openid.appauth.AppAuthConfiguration").kotlin,
            ),
        )
    }
}
