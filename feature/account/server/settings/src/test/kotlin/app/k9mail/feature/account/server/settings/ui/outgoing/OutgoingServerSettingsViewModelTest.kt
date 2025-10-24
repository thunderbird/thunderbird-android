package app.k9mail.feature.account.server.settings.ui.outgoing

import app.k9mail.core.ui.compose.testing.mvi.assertThatAndMviTurbinesConsumed
import app.k9mail.core.ui.compose.testing.mvi.eventStateTest
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthenticationType
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.common.domain.entity.MailConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.toSmtpDefaultPort
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.Effect
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.testing.coroutines.MainDispatcherRule
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.input.NumberInputField
import net.thunderbird.core.validation.input.StringInputField
import org.junit.Rule
import org.junit.Test

class OutgoingServerSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should load account setup state when LoadAccountState event is received`() = runMviTest {
        val accountState = AccountState(
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
        val repository = InMemoryAccountStateRepository(
            state = AccountState(),
        )
        val testSubject = createTestSubject(
            repository = repository,
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, State())

        repository.setState(accountState)

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
                ),
            )
        }
    }

    @Test
    fun `should change state when ServerChanged event is received`() = runMviTest {
        eventStateTest(
            viewModel = createTestSubject(State()),
            initialState = State(),
            event = Event.ServerChanged("server"),
            expectedState = State(server = StringInputField(value = "server")),
        )
    }

    @Test
    fun `should change security and port when SecurityChanged event is received`() = runMviTest {
        eventStateTest(
            viewModel = createTestSubject(State()),
            initialState = State(),
            event = Event.SecurityChanged(ConnectionSecurity.StartTLS),
            expectedState = State(
                security = ConnectionSecurity.StartTLS,
                port = NumberInputField(value = ConnectionSecurity.StartTLS.toSmtpDefaultPort()),
            ),
        )
    }

    @Test
    fun `should change state when PortChanged event is received`() = runMviTest {
        eventStateTest(
            viewModel = createTestSubject(State()),
            initialState = State(),
            event = Event.PortChanged(456L),
            expectedState = State(port = NumberInputField(value = 456L)),
        )
    }

    @Test
    fun `should change state when AuthenticationTypeChanged event is received`() = runMviTest {
        eventStateTest(
            viewModel = createTestSubject(State()),
            initialState = State(),
            event = Event.AuthenticationTypeChanged(AuthenticationType.PasswordEncrypted),
            expectedState = State(authenticationType = AuthenticationType.PasswordEncrypted),
        )
    }

    @Test
    fun `should change state when UsernameChanged event is received`() = runMviTest {
        eventStateTest(
            viewModel = createTestSubject(State()),
            initialState = State(),
            event = Event.UsernameChanged("username"),
            expectedState = State(username = StringInputField(value = "username")),
        )
    }

    @Test
    fun `should change state when PasswordChanged event is received`() = runMviTest {
        eventStateTest(
            viewModel = createTestSubject(State()),
            initialState = State(),
            event = Event.PasswordChanged("password"),
            expectedState = State(password = StringInputField(value = "password")),
        )
    }

    @Test
    fun `should change state when ClientCertificateChanged event is received`() = runMviTest {
        eventStateTest(
            viewModel = createTestSubject(State()),
            initialState = State(),
            event = Event.ClientCertificateChanged("clientCertificate"),
            expectedState = State(clientCertificateAlias = "clientCertificate"),
        )
    }

    @Test
    fun `should save state and emit effect NavigateNext when OnNextClicked is received and input valid`() = runMviTest {
        val initialState = State()
        val repository = InMemoryAccountStateRepository()
        val testSubject = createTestSubject(
            initialState = initialState,
            repository = repository,
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.OnNextClicked)

        assertThat(turbines.awaitStateItem()).isEqualTo(
            State(
                server = StringInputField(value = "", isValid = true),
                port = NumberInputField(value = 465L, isValid = true),
                authenticationType = AuthenticationType.PasswordCleartext,
                username = StringInputField(value = "", isValid = true),
                password = StringInputField(value = "", isValid = true),
            ),
        )

        assertThat(repository.getState()).isEqualTo(
            AccountState(
                outgoingServerSettings = ServerSettings(
                    type = "smtp",
                    host = "",
                    port = 465,
                    connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
                    authenticationType = AuthType.PLAIN,
                    username = "",
                    password = "",
                    clientCertificateAlias = null,
                    extra = emptyMap(),
                ),
            ),
        )

        assertThatAndMviTurbinesConsumed(
            actual = turbines.awaitEffectItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateNext)
        }
    }

    @Test
    fun `should save state and emit effect NavigateNext when OnNextClicked is received and input valid with OAuth`() =
        runMviTest {
            val initialState = State(
                authenticationType = AuthenticationType.OAuth2,
            )
            val repository = InMemoryAccountStateRepository()
            val testSubject = createTestSubject(
                initialState = initialState,
                repository = repository,
            )
            val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

            testSubject.event(Event.OnNextClicked)

            assertThat(turbines.awaitStateItem()).isEqualTo(
                State(
                    server = StringInputField(value = "", isValid = true),
                    port = NumberInputField(value = 465L, isValid = true),
                    authenticationType = AuthenticationType.OAuth2,
                    username = StringInputField(value = "", isValid = true),
                    password = StringInputField(value = "", isValid = true),
                ),
            )

            assertThat(repository.getState()).isEqualTo(
                AccountState(
                    outgoingServerSettings = ServerSettings(
                        type = "smtp",
                        host = "",
                        port = 465,
                        connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
                        authenticationType = AuthType.XOAUTH2,
                        username = "",
                        password = null,
                        clientCertificateAlias = null,
                        extra = emptyMap(),
                    ),
                ),
            )

            assertThatAndMviTurbinesConsumed(
                actual = turbines.awaitEffectItem(),
                turbines = turbines,
            ) {
                isEqualTo(Effect.NavigateNext)
            }
        }

    @Test
    fun `should change state and not emit NavigateNext effect when OnNextClicked event received and input invalid`() =
        runMviTest {
            val initialState = State()
            val testSubject = createTestSubject(
                validator = FakeOutgoingServerSettingsValidator(
                    serverAnswer = Outcome.Failure(TestError),
                ),
                initialState = initialState,
            )
            val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

            testSubject.event(Event.OnNextClicked)

            assertThatAndMviTurbinesConsumed(
                actual = turbines.awaitStateItem(),
                turbines = turbines,
            ) {
                isEqualTo(
                    State(
                        server = StringInputField(value = "", error = TestError, isValid = false),
                        port = NumberInputField(value = 465L, isValid = true),
                        username = StringInputField(value = "", isValid = true),
                        password = StringInputField(value = "", isValid = true),
                    ),
                )
            }
        }

    @Test
    fun `should emit NavigateBack effect when OnBackClicked event received`() = runMviTest {
        val initialState = State()
        val testSubject = createTestSubject(initialState)
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.OnBackClicked)

        assertThatAndMviTurbinesConsumed(
            actual = turbines.awaitEffectItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateBack)
        }
    }

    private object TestError : ValidationError

    private companion object {
        fun createTestSubject(
            initialState: State = State(),
            validator: OutgoingServerSettingsContract.Validator = FakeOutgoingServerSettingsValidator(),
            repository: AccountDomainContract.AccountStateRepository = InMemoryAccountStateRepository(),
        ) = OutgoingServerSettingsViewModel(
            mode = InteractionMode.Create,
            validator = validator,
            accountStateRepository = repository,
            initialState = initialState,
        )
    }
}
