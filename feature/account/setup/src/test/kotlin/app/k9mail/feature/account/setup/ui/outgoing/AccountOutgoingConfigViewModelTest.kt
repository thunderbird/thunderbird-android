package app.k9mail.feature.account.setup.ui.outgoing

import app.cash.turbine.testIn
import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.toSmtpDefaultPort
import app.k9mail.feature.account.setup.domain.input.NumberInputField
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.testing.eventStateTest
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Effect
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Error
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Event
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.State
import assertk.assertThat
import assertk.assertions.assertThatAndTurbinesConsumed
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AccountOutgoingConfigViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val testSubject = AccountOutgoingConfigViewModel(
        validator = FakeAccountOutgoingConfigValidator(),
        checkOutgoingServerConfig = {
            delay(50)
            ServerSettingsValidationResult.Success
        },
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
    fun `should emit effect NavigateNext when OnNextClicked is received in success state`() = runTest {
        val initialState = State(isSuccess = true)
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

        assertThatAndTurbinesConsumed(
            actual = effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateNext)
        }
    }

    @Test
    fun `should checkSettings when OnNextClicked event is received and input is valid`() =
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

            val validState = State(
                server = StringInputField(value = "", isValid = true),
                port = NumberInputField(value = 465L, isValid = true),
                username = StringInputField(value = "", isValid = true),
                password = StringInputField(value = "", isValid = true),
            )
            assertThat(stateTurbine.awaitItem()).isEqualTo(validState)

            val loadingState = validState.copy(isLoading = true)
            assertThat(stateTurbine.awaitItem()).isEqualTo(loadingState)

            val successState = loadingState.copy(
                isLoading = false,
                isSuccess = true,
            )
            assertThat(stateTurbine.awaitItem()).isEqualTo(successState)

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
                checkOutgoingServerConfig = { ServerSettingsValidationResult.Success },
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
    fun `should set error state when OnNextClicked and input valid but check settings failed`() = runTest {
        val viewModel = AccountOutgoingConfigViewModel(
            validator = FakeAccountOutgoingConfigValidator(),
            checkOutgoingServerConfig = { _ ->
                delay(50)
                ServerSettingsValidationResult.ServerError("server error")
            },
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

        val validState = State(
            server = StringInputField(value = "", isValid = true),
            port = NumberInputField(value = 465, isValid = true),
            username = StringInputField(value = "", isValid = true),
            password = StringInputField(value = "", isValid = true),
        )
        assertThat(stateTurbine.awaitItem()).isEqualTo(validState)

        val loadingState = validState.copy(isLoading = true)
        assertThat(stateTurbine.awaitItem()).isEqualTo(loadingState)

        val failureState = loadingState.copy(
            isLoading = false,
            error = Error.ServerError("server error"),
        )
        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(failureState)
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

    @Test
    fun `should clear isSuccess when OnBackClicked event received`() = runTest {
        val initialState = State(isSuccess = true)
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

        testSubject.event(Event.OnBackClicked)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(initialState.copy(isSuccess = false))
        }
    }

    @Test
    fun `should clear error when OnBackClicked event received`() = runTest {
        val initialState = State(error = Error.ServerError("server error"))
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

        testSubject.event(Event.OnBackClicked)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(initialState.copy(error = null))
        }
    }

    @Test
    fun `should clear error and trigger check settings when OnRetryClicked event received`() = runTest {
        val initialState = State(
            server = StringInputField(value = "", isValid = true),
            port = NumberInputField(value = 465, isValid = true),
            username = StringInputField(value = "", isValid = true),
            password = StringInputField(value = "", isValid = true),
            error = Error.ServerError("server error"),
        )
        var checkSettingsCalled = false
        val viewModel = AccountOutgoingConfigViewModel(
            validator = FakeAccountOutgoingConfigValidator(),
            checkOutgoingServerConfig = { _ ->
                checkSettingsCalled = true
                delay(50)
                ServerSettingsValidationResult.Success
            },
            initialState = initialState,
        )
        val stateTurbine = viewModel.state.testIn(backgroundScope)
        val effectTurbine = viewModel.effect.testIn(backgroundScope)
        val turbines = listOf(stateTurbine, effectTurbine)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(initialState)
        }

        viewModel.event(Event.OnRetryClicked)

        val stateWithoutError = initialState.copy(error = null)
        assertThat(stateTurbine.awaitItem()).isEqualTo(stateWithoutError)

        val loadingState = stateWithoutError.copy(isLoading = true)
        assertThat(stateTurbine.awaitItem()).isEqualTo(loadingState)

        val successState = loadingState.copy(isLoading = false, isSuccess = true)
        assertThat(stateTurbine.awaitItem()).isEqualTo(successState)
        assertThat(checkSettingsCalled).isTrue()

        assertThatAndTurbinesConsumed(
            actual = effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateNext)
        }
    }

    private object TestError : ValidationError
}
