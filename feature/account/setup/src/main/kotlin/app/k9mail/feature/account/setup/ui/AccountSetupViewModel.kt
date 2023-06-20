package app.k9mail.feature.account.setup.ui

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Effect
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Event
import app.k9mail.feature.account.setup.ui.AccountSetupContract.SetupStep
import app.k9mail.feature.account.setup.ui.AccountSetupContract.State
import app.k9mail.feature.account.setup.ui.AccountSetupContract.ViewModel
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract
import app.k9mail.feature.account.setup.ui.common.mapper.toIncomingConfigState
import app.k9mail.feature.account.setup.ui.common.mapper.toOptionsState
import app.k9mail.feature.account.setup.ui.common.mapper.toOutgoingConfigState

class AccountSetupViewModel(
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), ViewModel {

    override fun event(event: Event) {
        when (event) {
            is Event.OnAutoDiscoveryFinished -> handleAutoDiscoveryFinished(event.state)
            Event.OnBack -> onBack()
            Event.OnNext -> onNext()
        }
    }

    private fun handleAutoDiscoveryFinished(autoDiscoveryState: AccountAutoDiscoveryContract.State) {
        emitEffect(Effect.UpdateIncomingConfig(autoDiscoveryState.toIncomingConfigState()))
        emitEffect(Effect.UpdateOutgoingConfig(autoDiscoveryState.toOutgoingConfigState()))
        emitEffect(Effect.UpdateOptions(autoDiscoveryState.toOptionsState()))
        onNext()
    }

    private fun onBack() {
        when (state.value.setupStep) {
            SetupStep.AUTO_CONFIG -> navigateBack()
            SetupStep.INCOMING_CONFIG -> changeToSetupStep(SetupStep.AUTO_CONFIG)
            SetupStep.OUTGOING_CONFIG -> changeToSetupStep(SetupStep.INCOMING_CONFIG)
            SetupStep.OPTIONS -> changeToSetupStep(SetupStep.OUTGOING_CONFIG)
        }
    }

    private fun onNext() {
        when (state.value.setupStep) {
            SetupStep.AUTO_CONFIG -> changeToSetupStep(SetupStep.INCOMING_CONFIG)
            SetupStep.INCOMING_CONFIG -> changeToSetupStep(SetupStep.OUTGOING_CONFIG)
            SetupStep.OUTGOING_CONFIG -> changeToSetupStep(SetupStep.OPTIONS)
            SetupStep.OPTIONS -> navigateNext()
        }
    }

    private fun changeToSetupStep(setupStep: SetupStep) {
        updateState {
            it.copy(
                setupStep = setupStep,
            )
        }
    }

    private fun navigateNext() {
        // TODO: validate account

        emitEffect(Effect.NavigateNext)
    }

    private fun navigateBack() = emitEffect(Effect.NavigateBack)
}
