package app.k9mail.feature.account.server.settings.ui.incoming

import app.k9mail.core.ui.compose.testing.mvi.assertThatAndMviTurbinesConsumed
import app.k9mail.core.ui.compose.testing.mvi.eventStateTest
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthenticationType
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.common.domain.entity.MailConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.toImapDefaultPort
import app.k9mail.feature.account.common.domain.entity.toPop3DefaultPort
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Effect
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.testing.coroutines.MainDispatcherRule
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.input.NumberInputField
import net.thunderbird.core.validation.input.StringInputField
import org.junit.Rule
import org.junit.Test

class IncomingServerSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should load account setup state when LoadAccountState event is received`() = runMviTest {
        val accountState = AccountState(
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
                    protocolType = IncomingProtocolType.IMAP,
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

    @Test
    fun `should change protocol, security and port when ProtocolTypeChanged event is received`() = runMviTest {
        val initialState = State(
            security = ConnectionSecurity.StartTLS,
            port = NumberInputField(value = ConnectionSecurity.StartTLS.toImapDefaultPort()),
        )
        val testSubject = createTestSubject(initialState)

        eventStateTest(
            viewModel = testSubject,
            initialState = initialState,
            event = Event.ProtocolTypeChanged(IncomingProtocolType.POP3),
            expectedState = State(
                protocolType = IncomingProtocolType.POP3,
                security = ConnectionSecurity.TLS,
                port = NumberInputField(value = ConnectionSecurity.TLS.toPop3DefaultPort()),
            ),
        )
    }

    @Test
    fun `should change state when ServerChanged event is received`() = runMviTest {
        val initialState = State()
        eventStateTest(
            viewModel = createTestSubject(initialState),
            initialState = initialState,
            event = Event.ServerChanged("server"),
            expectedState = State(server = StringInputField(value = "server")),
        )
    }

    @Test
    fun `should change security and port when SecurityChanged event is received`() = runMviTest {
        val initialState = State()
        eventStateTest(
            viewModel = createTestSubject(initialState),
            initialState = initialState,
            event = Event.SecurityChanged(ConnectionSecurity.StartTLS),
            expectedState = State(
                security = ConnectionSecurity.StartTLS,
                port = NumberInputField(value = ConnectionSecurity.StartTLS.toImapDefaultPort()),
            ),
        )
    }

    @Test
    fun `should change state when PortChanged event is received`() = runMviTest {
        val initialState = State()
        eventStateTest(
            viewModel = createTestSubject(initialState),
            initialState = initialState,
            event = Event.PortChanged(456L),
            expectedState = State(port = NumberInputField(value = 456L)),
        )
    }

    @Test
    fun `should change authentication type when AuthenticationTypeChanged event is received`() = runMviTest {
        val initialState = State()
        eventStateTest(
            viewModel = createTestSubject(initialState),
            initialState = initialState,
            event = Event.AuthenticationTypeChanged(AuthenticationType.PasswordEncrypted),
            expectedState = State(authenticationType = AuthenticationType.PasswordEncrypted),
        )
    }

    @Test
    fun `should change state when UsernameChanged event is received`() = runMviTest {
        val initialState = State()
        eventStateTest(
            viewModel = createTestSubject(initialState),
            initialState = initialState,
            event = Event.UsernameChanged("username"),
            expectedState = State(username = StringInputField(value = "username")),
        )
    }

    @Test
    fun `should change state when PasswordChanged event is received`() = runMviTest {
        val initialState = State()
        eventStateTest(
            viewModel = createTestSubject(initialState),
            initialState = initialState,
            event = Event.PasswordChanged("password"),
            expectedState = State(password = StringInputField(value = "password")),
        )
    }

    @Test
    fun `should change state when ClientCertificateChanged event is received`() = runMviTest {
        val initialState = State()
        eventStateTest(
            viewModel = createTestSubject(initialState),
            initialState = initialState,
            event = Event.ClientCertificateChanged("clientCertificate"),
            expectedState = State(clientCertificateAlias = "clientCertificate"),
        )
    }

    @Test
    fun `should change state when ImapAutoDetectNamespaceChanged event is received`() = runMviTest {
        val initialState = State(imapAutodetectNamespaceEnabled = true)
        eventStateTest(
            viewModel = createTestSubject(initialState),
            initialState = State(imapAutodetectNamespaceEnabled = true),
            event = Event.ImapAutoDetectNamespaceChanged(false),
            expectedState = State(imapAutodetectNamespaceEnabled = false),
        )
    }

    @Test
    fun `should change state when ImapPrefixChanged event is received`() = runMviTest {
        val initialState = State()
        eventStateTest(
            viewModel = createTestSubject(initialState),
            initialState = initialState,
            event = Event.ImapPrefixChanged("imapPrefix"),
            expectedState = State(imapPrefix = StringInputField(value = "imapPrefix")),
        )
    }

    @Test
    fun `should change state when ImapUseCompressionChanged event is received`() = runMviTest {
        val initialState = State(imapUseCompression = true)
        eventStateTest(
            viewModel = createTestSubject(initialState),
            initialState = initialState,
            event = Event.ImapUseCompressionChanged(false),
            expectedState = State(imapUseCompression = false),
        )
    }

    @Test
    fun `should change state when ImapSendClientInfoChanged event is received`() = runMviTest {
        val initialState = State(imapSendClientInfo = true)
        eventStateTest(
            viewModel = createTestSubject(initialState),
            initialState = initialState,
            event = Event.ImapSendClientInfoChanged(false),
            expectedState = State(imapSendClientInfo = false),
        )
    }

    @Test
    fun `should save state emit effect NavigateNext when OnNextClicked is received and input valid`() = runMviTest {
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
                protocolType = IncomingProtocolType.IMAP,
                server = StringInputField(value = "", isValid = true),
                port = NumberInputField(value = 993L, isValid = true),
                authenticationType = AuthenticationType.PasswordCleartext,
                username = StringInputField(value = "", isValid = true),
                password = StringInputField(value = "", isValid = true),
                imapPrefix = StringInputField(value = "", isValid = true),
            ),
        )

        assertThat(repository.getState()).isEqualTo(
            AccountState(
                incomingServerSettings = ServerSettings(
                    type = "imap",
                    host = "",
                    port = 993,
                    connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
                    authenticationType = AuthType.PLAIN,
                    username = "",
                    password = "",
                    clientCertificateAlias = null,
                    extra = ImapStoreSettings.createExtra(
                        autoDetectNamespace = true,
                        pathPrefix = null,
                        useCompression = true,
                        sendClientInfo = true,
                    ),
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
                    protocolType = IncomingProtocolType.IMAP,
                    server = StringInputField(value = "", isValid = true),
                    port = NumberInputField(value = 993L, isValid = true),
                    authenticationType = AuthenticationType.OAuth2,
                    username = StringInputField(value = "", isValid = true),
                    password = StringInputField(value = "", isValid = true),
                    imapPrefix = StringInputField(value = "", isValid = true),
                ),
            )

            assertThat(repository.getState()).isEqualTo(
                AccountState(
                    emailAddress = null,
                    incomingServerSettings = ServerSettings(
                        type = "imap",
                        host = "",
                        port = 993,
                        connectionSecurity = MailConnectionSecurity.SSL_TLS_REQUIRED,
                        authenticationType = AuthType.XOAUTH2,
                        username = "",
                        password = null,
                        clientCertificateAlias = null,
                        extra = ImapStoreSettings.createExtra(
                            autoDetectNamespace = true,
                            pathPrefix = null,
                            useCompression = true,
                            sendClientInfo = true,
                        ),
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
            val testSubject = IncomingServerSettingsViewModel(
                mode = InteractionMode.Create,
                validator = FakeIncomingServerSettingsValidator(
                    serverAnswer = Outcome.Failure(TestError),
                ),
                accountStateRepository = InMemoryAccountStateRepository(),
            )
            val turbines = turbinesWithInitialStateCheck(testSubject, State())

            testSubject.event(Event.OnNextClicked)

            assertThatAndMviTurbinesConsumed(
                actual = turbines.awaitStateItem(),
                turbines = turbines,
            ) {
                isEqualTo(
                    State(
                        server = StringInputField(value = "", error = TestError, isValid = false),
                        port = NumberInputField(value = 993L, isValid = true),
                        username = StringInputField(value = "", isValid = true),
                        password = StringInputField(value = "", isValid = true),
                        imapPrefix = StringInputField(value = "", isValid = true),
                    ),
                )
            }
        }

    @Test
    fun `should emit NavigateBack effect when OnBackClicked event received`() = runMviTest {
        val testSubject = createTestSubject(State())
        val turbines = turbinesWithInitialStateCheck(testSubject, State())

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
            validator: IncomingServerSettingsContract.Validator = FakeIncomingServerSettingsValidator(),
            repository: AccountDomainContract.AccountStateRepository = InMemoryAccountStateRepository(),
        ) = IncomingServerSettingsViewModel(
            mode = InteractionMode.Create,
            validator = validator,
            accountStateRepository = repository,
            initialState = initialState,
        )
    }
}
