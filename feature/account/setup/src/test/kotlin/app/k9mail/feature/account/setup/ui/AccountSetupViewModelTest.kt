package app.k9mail.feature.account.setup.ui

import app.cash.turbine.testIn
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Effect
import app.k9mail.feature.account.setup.ui.AccountSetupContract.SetupStep
import app.k9mail.feature.account.setup.ui.AccountSetupContract.State
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract
import assertk.assertThat
import assertk.assertions.assertThatAndTurbinesConsumed
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AccountSetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Suppress("LongMethod")
    @Test
    fun `should forward step state on next event`() = runTest {
        val viewModel = AccountSetupViewModel(
            createAccount = { _, _, _, _ -> "accountUuid" },
        )
        val stateTurbine = viewModel.state.testIn(backgroundScope)
        val effectTurbine = viewModel.effect.testIn(backgroundScope)
        val turbines = listOf(stateTurbine, effectTurbine)

        // Initial state
        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.AUTO_CONFIG)
        }

        viewModel.event(
            AccountSetupContract.Event.OnAutoDiscoveryFinished(
                state = AccountAutoDiscoveryContract.State(),
                isAutomaticConfig = false,
            ),
        )

        assertThat(effectTurbine.awaitItem())
            .isEqualTo(Effect.UpdateIncomingConfig(AccountIncomingConfigContract.State()))

        assertThat(effectTurbine.awaitItem())
            .isEqualTo(Effect.UpdateOutgoingConfig(AccountOutgoingConfigContract.State()))

        assertThat(effectTurbine.awaitItem())
            .isEqualTo(Effect.UpdateOptions(AccountOptionsContract.State()))

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.INCOMING_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnNext)

        assertThat(effectTurbine.awaitItem()).isEqualTo(Effect.UpdateIncomingConfigValidation)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.INCOMING_VALIDATION)
        }

        viewModel.event(AccountSetupContract.Event.OnNext)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.OUTGOING_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnNext)

        assertThat(effectTurbine.awaitItem()).isEqualTo(Effect.UpdateOutgoingConfigValidation)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.OUTGOING_VALIDATION)
        }

        viewModel.event(AccountSetupContract.Event.OnNext)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.OPTIONS)
        }

        viewModel.event(AccountSetupContract.Event.OnNext)

        assertThatAndTurbinesConsumed(
            actual = effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.CollectExternalStates)
        }

        viewModel.event(
            AccountSetupContract.Event.OnStateCollected(
                autoDiscoveryState = AccountAutoDiscoveryContract.State(),
                incomingState = AccountIncomingConfigContract.State(),
                outgoingState = AccountOutgoingConfigContract.State(),
                optionsState = AccountOptionsContract.State(),
            ),
        )

        assertThatAndTurbinesConsumed(
            actual = effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateNext("accountUuid"))
        }
    }

    @Test
    fun `should rewind step state on back event`() = runTest {
        val initialState = State(setupStep = SetupStep.OPTIONS)
        val viewModel = AccountSetupViewModel(
            createAccount = { _, _, _, _ -> "accountUuid" },
            initialState = initialState,
        )
        val stateTurbine = viewModel.state.testIn(backgroundScope)
        val effectTurbine = viewModel.effect.testIn(backgroundScope)
        val turbines = listOf(stateTurbine, effectTurbine)

        // Initial state
        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.OPTIONS)
        }

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.OUTGOING_VALIDATION)
        }

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.OUTGOING_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.INCOMING_VALIDATION)
        }

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.INCOMING_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndTurbinesConsumed(
            actual = stateTurbine.awaitItem(),
            turbines = turbines,
        ) {
            prop(State::setupStep).isEqualTo(SetupStep.AUTO_CONFIG)
        }

        viewModel.event(AccountSetupContract.Event.OnBack)

        assertThatAndTurbinesConsumed(
            actual = effectTurbine.awaitItem(),
            turbines = turbines,
        ) {
            isEqualTo(Effect.NavigateBack)
        }
    }
}
