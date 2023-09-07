package app.k9mail.feature.account.setup.ui.options

import app.cash.turbine.testIn
import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.core.ui.compose.testing.mvi.eventStateTest
import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.Effect
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.Event
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.State
import assertk.assertThat
import assertk.assertions.assertThatAndTurbinesConsumed
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AccountOptionsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val testSubject = AccountOptionsViewModel(
        validator = FakeAccountOptionsValidator(),
        accountStateRepository = InMemoryAccountStateRepository(),
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
    fun `should change state when OnCheckFrequencyChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.OnCheckFrequencyChanged(EmailCheckFrequency.EVERY_12_HOURS),
            expectedState = State(checkFrequency = EmailCheckFrequency.EVERY_12_HOURS),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when OnMessageDisplayCountChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.OnMessageDisplayCountChanged(EmailDisplayCount.MESSAGES_1000),
            expectedState = State(messageDisplayCount = EmailDisplayCount.MESSAGES_1000),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when OnShowNotificationChanged event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(),
            event = Event.OnShowNotificationChanged(true),
            expectedState = State(showNotification = true),
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
            val viewModel = AccountOptionsViewModel(
                validator = FakeAccountOptionsValidator(
                    accountNameAnswer = ValidationResult.Failure(TestError),
                ),
                accountStateRepository = InMemoryAccountStateRepository(),
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

    private object TestError : ValidationError
}
