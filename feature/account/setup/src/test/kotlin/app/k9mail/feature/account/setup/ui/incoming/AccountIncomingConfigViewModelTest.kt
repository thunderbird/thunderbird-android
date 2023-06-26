package app.k9mail.feature.account.setup.ui.incoming

import androidx.lifecycle.viewmodel.compose.viewModel
import app.cash.turbine.testIn
import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.setup.domain.entity.toImapDefaultPort
import app.k9mail.feature.account.setup.domain.entity.toPop3DefaultPort
import app.k9mail.feature.account.setup.domain.input.NumberInputField
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.testing.eventStateTest
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Effect
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Event
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.State
import assertk.assertThat
import assertk.assertions.assertThatAndTurbinesConsumed
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountIncomingConfigViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val testSubject = AccountIncomingConfigViewModel(
        validator = FakeAccountIncomingConfigValidator(),
        checkIncomingServerConfig = { _, _ -> ServerSettingsValidationResult.Success },
    )

    @Test
    fun `should change protocol, security and port when ProtocolTypeChanged event is received`() = runTest {
        val initialState = State(
            security = ConnectionSecurity.StartTLS,
            port = NumberInputField(value = ConnectionSecurity.StartTLS.toImapDefaultPort()),
        )
        testSubject.initState(initialState)

        eventStateTest(
            viewModel = testSubject,
            initialState = initialState,
            event = Event.ProtocolTypeChanged(IncomingProtocolType.POP3),
            expectedState = State(
                protocolType = IncomingProtocolType.POP3,
                security = ConnectionSecurity.TLS,
                port = NumberInputField(value = ConnectionSecurity.TLS.toPop3DefaultPort()),
            ),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when ServerChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.ServerChanged("server"),
            expectedState = State(server = StringInputField(value = "server")),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change security and port when SecurityChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.SecurityChanged(ConnectionSecurity.StartTLS),
            expectedState = State(
                security = ConnectionSecurity.StartTLS,
                port = NumberInputField(value = ConnectionSecurity.StartTLS.toImapDefaultPort()),
            ),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when PortChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.PortChanged(456L),
            expectedState = State(port = NumberInputField(value = 456L)),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when UsernameChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.UsernameChanged("username"),
            expectedState = State(username = StringInputField(value = "username")),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when PasswordChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.PasswordChanged("password"),
            expectedState = State(password = StringInputField(value = "password")),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when ClientCertificateChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.ClientCertificateChanged("clientCertificate"),
            expectedState = State(clientCertificate = "clientCertificate"),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when ImapAutoDetectNamespaceChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(imapAutodetectNamespaceEnabled = true),
            event = Event.ImapAutoDetectNamespaceChanged(false),
            expectedState = State(imapAutodetectNamespaceEnabled = false),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when ImapPrefixChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.ImapPrefixChanged("imapPrefix"),
            expectedState = State(imapPrefix = StringInputField(value = "imapPrefix")),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when UseCompressionChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(useCompression = true),
            event = Event.UseCompressionChanged(false),
            expectedState = State(useCompression = false),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state and emit NavigateNext effect when OnNextClicked event is received and input is valid`() =
        runTest {
            val viewModel = testSubject
            val stateTurbine = viewModel.state.testIn(backgroundScope)
            val effectTurbine = viewModel.effect.testIn(backgroundScope)
            val turbines = listOf(stateTurbine, effectTurbine)

            assertThatAndTurbinesConsumed(
                actual = stateTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(State())
            }

            viewModel.event(Event.OnNextClicked)

            assertThat(stateTurbine.awaitItem()).isEqualTo(
                State(
                    server = StringInputField(value = "", isValid = true),
                    port = NumberInputField(value = 993L, isValid = true),
                    username = StringInputField(value = "", isValid = true),
                    password = StringInputField(value = "", isValid = true),
                    imapPrefix = StringInputField(value = "", isValid = true),
                ),
            )

            assertThatAndTurbinesConsumed(
                actual = effectTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(Effect.NavigateNext)
            }
        }

    @Test
    fun `should change state and not emit NavigateNext effect when OnNextClicked event received and input invalid`() =
        runTest {
            val viewModel = AccountIncomingConfigViewModel(
                validator = FakeAccountIncomingConfigValidator(
                    serverAnswer = ValidationResult.Failure(TestError),
                ),
                checkIncomingServerConfig = { _, _ -> ServerSettingsValidationResult.Success },
            )
            val stateTurbine = viewModel.state.testIn(backgroundScope)
            val effectTurbine = viewModel.effect.testIn(backgroundScope)
            val turbines = listOf(stateTurbine, effectTurbine)

            assertThatAndTurbinesConsumed(
                actual = stateTurbine.awaitItem(),
                turbines = turbines,
            ) {
                isEqualTo(State())
            }

            viewModel.event(Event.OnNextClicked)

            assertThatAndTurbinesConsumed(
                actual = stateTurbine.awaitItem(),
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
    fun `should emit NavigateBack effect when OnBackClicked event received`() = runTest {
        val viewModel = testSubject
        val stateTurbine = viewModel.state.testIn(backgroundScope)
        val effectTurbine = viewModel.effect.testIn(backgroundScope)
        val turbines = listOf(stateTurbine, effectTurbine)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(State())
        }

        viewModel.event(Event.OnBackClicked)

        assertThatAndTurbinesConsumed(
            actual = effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateBack)
        }
    }

    private object TestError : ValidationError
}
