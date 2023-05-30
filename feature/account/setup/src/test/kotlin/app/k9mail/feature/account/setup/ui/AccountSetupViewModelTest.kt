package app.k9mail.feature.account.setup.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.testIn
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Effect.NavigateBack
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Effect.NavigateNext
import app.k9mail.feature.account.setup.ui.AccountSetupContract.SetupStep
import app.k9mail.feature.account.setup.ui.AccountSetupContract.State
import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should forward step state on next event`() = runTest {
        val viewModel = AccountSetupViewModel()
        val stateTurbine = viewModel.state.testIn(backgroundScope)
        val effectTurbine = viewModel.effect.testIn(backgroundScope)
        val turbines = listOf(stateTurbine, effectTurbine)

        // Initial state
        assertThatAndAllEventsConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.AUTO_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnNext)

        assertThatAndAllEventsConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.INCOMING_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnNext)

        assertThatAndAllEventsConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.OUTGOING_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnNext)

        assertThatAndAllEventsConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.OPTIONS)
        }

        viewModel.event(AccountSetupContract.Event.OnNext)

        assertThatAndAllEventsConsumed(
            actual = effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(NavigateNext)
        }
    }

    @Test
    fun `should rewind step state on back event`() = runTest {
        val initialState = State(setupStep = SetupStep.OPTIONS)
        val viewModel = AccountSetupViewModel(initialState)
        val stateTurbine = viewModel.state.testIn(backgroundScope)
        val effectTurbine = viewModel.effect.testIn(backgroundScope)
        val turbines = listOf(stateTurbine, effectTurbine)

        // Initial state
        assertThatAndAllEventsConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.OPTIONS)
        }

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndAllEventsConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.OUTGOING_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndAllEventsConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.INCOMING_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndAllEventsConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.AUTO_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndAllEventsConsumed(
            actual = effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(NavigateBack)
        }
    }

    private fun <T> assertThatAndAllEventsConsumed(
        actual: T,
        turbines: List<ReceiveTurbine<*>>,
        assertion: Assert<T>.() -> Unit,
    ) {
        assertThat(actual).all {
            assertion()
        }

        turbines.forEach { it.ensureAllEventsConsumed() }
    }
}
