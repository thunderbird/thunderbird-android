package app.k9mail.feature.account.edit

import android.content.Context
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsContract
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract
import net.thunderbird.feature.account.AccountId
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.KoinTest
import org.koin.test.verify.verify

class AccountEditModuleKtTest : KoinTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `should have a valid di module`() {
        featureAccountEditModule.verify(
            extraTypes = listOf(
                AccountId::class,
                Context::class,
                AccountState::class,
                Class.forName("net.openid.appauth.AppAuthConfiguration").kotlin,
                ServerValidationContract.State::class,
                ServerCertificateErrorContract.State::class,
                IncomingServerSettingsContract.State::class,
                OutgoingServerSettingsContract.State::class,
                SaveServerSettingsContract.State::class,
                AccountEditExternalContract.AccountServerSettingsUpdater::class,
                InteractionMode::class,
            ),
        )
    }
}
