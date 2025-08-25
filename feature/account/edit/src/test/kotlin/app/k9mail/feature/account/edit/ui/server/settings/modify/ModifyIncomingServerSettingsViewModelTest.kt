package app.k9mail.feature.account.edit.ui.server.settings.modify

import app.k9mail.core.ui.compose.testing.mvi.assertThatAndMviTurbinesConsumed
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthenticationType
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.MailConnectionSecurity
import app.k9mail.feature.account.common.domain.input.NumberInputField
import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings
import kotlinx.coroutines.delay
import net.thunderbird.core.testing.coroutines.MainDispatcherRule
import org.junit.Rule
import org.junit.Test

class ModifyIncomingServerSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should load account state from use case`() = runMviTest {
        val accountUuid = "accountUuid"
        val accountState = AccountState(
            uuid = "accountUuid",
            emailAddress = "test@example.com",
            incomingServerSettings = ServerSettings(
                "imap",
                "imap.example.com",
                123,
                MailConnectionSecurity.SSL_TLS_REQUIRED,
                AuthType.PLAIN,
                "username",
                "password",
                clientCertificateAlias = null,
                extra = ImapStoreSettings.createExtra(
                    autoDetectNamespace = true,
                    pathPrefix = null,
                    useCompression = true,
                    sendClientInfo = true,
                ),
            ),
        )

        val testSubject = ModifyIncomingServerSettingsViewModel(
            accountUuid = accountUuid,
            accountStateLoader = { _ ->
                delay(50)
                accountState
            },
            validator = FakeIncomingServerSettingsValidator(),
            accountStateRepository = InMemoryAccountStateRepository(),
            initialState = State(),
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, State())

        testSubject.event(Event.LoadAccountState)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.awaitStateItem(),
            turbines = turbines,
        ) {
            isEqualTo(
                State(
                    server = StringInputField(value = "imap.example.com"),
                    security = ConnectionSecurity.TLS,
                    port = NumberInputField(value = 123L),
                    authenticationType = AuthenticationType.PasswordCleartext,
                    username = StringInputField(value = "username"),
                    password = StringInputField(value = "password"),
                    imapAutodetectNamespaceEnabled = true,
                    imapPrefix = StringInputField(value = ""),
                    imapUseCompression = true,
                    imapSendClientInfo = true,
                ),
            )
        }
    }
}
