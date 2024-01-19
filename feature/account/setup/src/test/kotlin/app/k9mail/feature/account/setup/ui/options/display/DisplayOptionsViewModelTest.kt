package app.k9mail.feature.account.setup.ui.options.display

import app.cash.turbine.testIn
import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.core.ui.compose.testing.mvi.eventStateTest
import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.Effect
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.Event
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.State
import assertk.assertThat
import assertk.assertions.assertThatAndTurbinesConsumed
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class DisplayOptionsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountOwnerNameProvider = FakeAccountOwnerNameProvider()
    private val testSubject = DisplayOptionsViewModel(
        validator = FakeDisplayOptionsValidator(),
        accountStateRepository = InMemoryAccountStateRepository(),
        accountOwnerNameProvider = accountOwnerNameProvider,
    )

    @Test
    fun `should change state when OnAccountNameChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.OnAccountNameChanged("accountName"),
            expectedState = State(accountName = StringInputField(value = "accountName")),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when OnDisplayNameChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.OnDisplayNameChanged("displayName"),
            expectedState = State(displayName = StringInputField(value = "displayName")),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when OnEmailSignatureChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.OnEmailSignatureChanged("emailSignature"),
            expectedState = State(emailSignature = StringInputField(value = "emailSignature")),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state and emit NavigateNext effect when OnNextClicked event received and input valid`() =
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
                    accountName = StringInputField(value = "", isValid = true),
                    displayName = StringInputField(value = "", isValid = true),
                    emailSignature = StringInputField(value = "", isValid = true),
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
    fun `should change state and not emit effect when OnNextClicked event received and input invalid`() =
        runTest {
            val viewModel = DisplayOptionsViewModel(
                validator = FakeDisplayOptionsValidator(
                    accountNameAnswer = ValidationResult.Failure(TestError),
                ),
                accountStateRepository = InMemoryAccountStateRepository(),
                accountOwnerNameProvider = accountOwnerNameProvider,
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
                        accountName = StringInputField(value = "", error = TestError, isValid = false),
                        displayName = StringInputField(value = "", isValid = true),
                        emailSignature = StringInputField(value = "", isValid = true),
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

    @Test
    fun `should set owner name when LoadAccountState event received`() = runTest {
        accountOwnerNameProvider.ownerName = "Alice Example"
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

        viewModel.event(Event.LoadAccountState)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(State(displayName = StringInputField("Alice Example")))
        }
    }

    private object TestError : ValidationError
}
