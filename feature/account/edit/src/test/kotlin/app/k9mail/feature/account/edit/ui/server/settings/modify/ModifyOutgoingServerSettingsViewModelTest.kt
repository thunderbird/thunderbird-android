package app.k9mail.feature.account.edit.ui.server.settings.modify

import app.k9mail.core.ui.compose.testing.MainDispatcherRule
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
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.State
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test

class ModifyOutgoingServerSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should load account state from use case`() = runMviTest {
        val accountUuid = "accountUuid"
        val accountState = AccountState(
            uuid = "accountUuid",
            emailAddress = "test@example.com",
            outgoingServerSettings = ServerSettings(
                "smtp",
                "smtp.example.com",
                123,
                MailConnectionSecurity.SSL_TLS_REQUIRED,
                AuthType.PLAIN,
                "username",
                "password",
                clientCertificateAlias = null,
                extra = emptyMap(),
            ),
        )
        val testSubject = ModifyOutgoingServerSettingsViewModel(
            accountUuid = accountUuid,
            accountStateLoader = { _ ->
                delay(50)
                accountState
            },
            validator = FakeOutgoingServerSettingsValidator(),
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
                    server = StringInputField(value = "smtp.example.com"),
                    security = ConnectionSecurity.TLS,
                    port = NumberInputField(value = 123L),
                    authenticationType = AuthenticationType.PasswordCleartext,
                    username = StringInputField(value = "username"),
                    password = StringInputField(value = "password"),

                    isLoading = false,
                ),
            )
        }
    }
}
