package app.k9mail.feature.account.setup.ui.options

import app.cash.turbine.testIn
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.Event
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.State
import assertk.assertions.assertThatAndTurbinesConsumed
import assertk.assertions.isEqualTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AccountOptionsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should change state when OnAccountNameChanged event is received`() = runTest {
        eventStateTest(
            event = Event.OnAccountNameChanged("accountName"),
            expectedState = State(accountName = StringInputField(value = "accountName")),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when OnDisplayNameChanged event is received`() = runTest {
        eventStateTest(
            event = Event.OnDisplayNameChanged("displayName"),
            expectedState = State(displayName = StringInputField(value = "displayName")),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when OnEmailSignatureChanged event is received`() = runTest {
        eventStateTest(
            event = Event.OnEmailSignatureChanged("emailSignature"),
            expectedState = State(emailSignature = StringInputField(value = "emailSignature")),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when OnCheckFrequencyChanged event is received`() = runTest {
        eventStateTest(
            event = Event.OnCheckFrequencyChanged(EmailCheckFrequency.EVERY_12_HOURS),
            expectedState = State(checkFrequency = EmailCheckFrequency.EVERY_12_HOURS),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when OnMessageDisplayCountChanged event is received`() = runTest {
        eventStateTest(
            event = Event.OnMessageDisplayCountChanged(EmailDisplayCount.MESSAGES_1000),
            expectedState = State(messageDisplayCount = EmailDisplayCount.MESSAGES_1000),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should change state when OnShowNotificationChanged event is received`() = runTest {
        eventStateTest(
            event = Event.OnShowNotificationChanged(true),
            expectedState = State(showNotification = true),
            coroutineScope = backgroundScope,
        )
    }

    @Test
    fun `should emit NavigateNext effect when OnNextClicked event is received`() = runTest {
        val viewModel = AccountOptionsViewModel()
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
            actual = effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(AccountOptionsContract.Effect.NavigateNext)
        }
    }

    @Test
    fun `should emit NavigateBack effect when OnBackClicked event is received`() = runTest {
        val viewModel = AccountOptionsViewModel()
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
            isEqualTo(AccountOptionsContract.Effect.NavigateBack)
        }
    }

    private suspend fun eventStateTest(
        event: Event,
        expectedState: State,
        coroutineScope: CoroutineScope,
    ) {
        val viewModel = AccountOptionsViewModel()
        val stateTurbine = viewModel.state.testIn(coroutineScope)
        val effectTurbine = viewModel.effect.testIn(coroutineScope)
        val turbines = listOf(stateTurbine, effectTurbine)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(State())
        }

        viewModel.event(event)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(expectedState)
        }
    }
}
