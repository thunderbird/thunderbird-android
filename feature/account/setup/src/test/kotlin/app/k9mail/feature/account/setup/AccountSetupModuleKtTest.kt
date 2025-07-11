package app.k9mail.feature.account.setup

import android.content.Context
import app.k9mail.feature.account.common.AccountCommonExternalContract
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract
import com.fsck.k9.mail.oauth.AuthStateStorage
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.verify.verify

class AccountSetupModuleKtTest : KoinTest {

    @Test
    fun `should have a valid di module`() {
        featureAccountSetupModule.verify(
            extraTypes = listOf(
                AccountCommonExternalContract.AccountStateLoader::class,
                AccountAutoDiscoveryContract.State::class,
                AccountOAuthContract.State::class,
                ServerValidationContract.State::class,
                IncomingServerSettingsContract.State::class,
                OutgoingServerSettingsContract.State::class,
                DisplayOptionsContract.State::class,
                SyncOptionsContract.State::class,
                AccountState::class,
                ServerCertificateErrorContract.State::class,
                AuthStateStorage::class,
                Context::class,
                Boolean::class,
                Class.forName("net.openid.appauth.AppAuthConfiguration").kotlin,
                InteractionMode::class,
                SpecialFoldersContract.State::class,
                CreateAccountContract.State::class,
                AccountSetupExternalContract.AccountOwnerNameProvider::class,
            ),
        )
    }
}
