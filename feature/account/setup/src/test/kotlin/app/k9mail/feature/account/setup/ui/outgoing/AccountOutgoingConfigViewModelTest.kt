package app.k9mail.feature.account.setup.ui.outgoing

import app.cash.turbine.testIn
import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.core.ui.compose.testing.mvi.eventStateTest
import app.k9mail.feature.account.setup.domain.entity.AuthenticationType
import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.toSmtpDefaultPort
import app.k9mail.feature.account.setup.domain.input.NumberInputField
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Effect
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Event
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.State
import assertk.assertThat
import assertk.assertions.assertThatAndTurbinesConsumed
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AccountOutgoingConfigViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val testSubject = AccountOutgoingConfigViewModel(
        validator = FakeAccountOutgoingConfigValidator(),
    )

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
                port = NumberInputField(value = ConnectionSecurity.StartTLS.toSmtpDefaultPort()),
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
    fun `should change state when AuthenticationTypeChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.AuthenticationTypeChanged(AuthenticationType.PasswordEncrypted),
            expectedState = State(authenticationType = AuthenticationType.PasswordEncrypted),
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
            expectedState = State(clientCertificateAlias = "clientCertificate"),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should emit effect NavigateNext when OnNextClicked is received and input valid`() = runTest {
        val initialState = State()
        testSubject.initState(initialState)
        val stateTurbine = testSubject.state.testIn(backgroundScope)
        val effectTurbine = testSubject.effect.testIn(backgroundScope)
        val turbines = listOf(stateTurbine, effectTurbine)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(initialState)
        }

        testSubject.event(Event.OnNextClicked)

        assertThat(stateTurbine.awaitItem()).isEqualTo(
            State(
                server = StringInputField(value = "", isValid = true),
                port = NumberInputField(value = 465L, isValid = true),
                username = StringInputField(value = "", isValid = true),
                password = StringInputField(value = "", isValid = true),
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
            val viewModel = AccountOutgoingConfigViewModel(
                validator = FakeAccountOutgoingConfigValidator(
                    serverAnswer = ValidationResult.Failure(TestError),
                ),
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
                        port = NumberInputField(value = 465L, isValid = true),
                        username = StringInputField(value = "", isValid = true),
                        password = StringInputField(value = "", isValid = true),
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
