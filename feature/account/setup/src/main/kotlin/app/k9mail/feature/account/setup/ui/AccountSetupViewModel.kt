package app.k9mail.feature.account.setup.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Effect
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Event
import app.k9mail.feature.account.setup.ui.AccountSetupContract.SetupStep
import app.k9mail.feature.account.setup.ui.AccountSetupContract.State
import app.k9mail.feature.account.setup.ui.AccountSetupContract.ViewModel
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract
import app.k9mail.feature.account.setup.ui.common.mapper.toIncomingConfigState
import app.k9mail.feature.account.setup.ui.common.mapper.toOptionsState
import app.k9mail.feature.account.setup.ui.common.mapper.toOutgoingConfigState
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract
import app.k9mail.feature.account.setup.ui.incoming.toServerSettings
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract
import app.k9mail.feature.account.setup.ui.options.toAccountOptions
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract
import app.k9mail.feature.account.setup.ui.outgoing.toServerSettings
import kotlinx.coroutines.launch

class AccountSetupViewModel(
    private val createAccount: UseCase.CreateAccount,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), ViewModel {

    override fun event(event: Event) {
        when (event) {
            is Event.OnAutoDiscoveryFinished -> {
                updateState {
                    it.copy(
                        isAutomaticConfig = event.isAutomaticConfig,
                    )
                }
                onAutoDiscoveryFinished(event.state)
            }

            is Event.OnStateCollected -> onStateCollected(
                autoDiscoveryState = event.autoDiscoveryState,
                incomingState = event.incomingState,
                outgoingState = event.outgoingState,
                optionsState = event.optionsState,
            )

            Event.OnBack -> onBack()
            Event.OnNext -> onNext()
        }
    }

    private fun onAutoDiscoveryFinished(
        autoDiscoveryState: AccountAutoDiscoveryContract.State,
    ) {
        emitEffect(Effect.UpdateIncomingConfig(autoDiscoveryState.toIncomingConfigState()))
        emitEffect(Effect.UpdateOutgoingConfig(autoDiscoveryState.toOutgoingConfigState()))
        emitEffect(Effect.UpdateOptions(autoDiscoveryState.toOptionsState()))
        onNext()
    }

    private fun onBack() {
        when (state.value.setupStep) {
            SetupStep.AUTO_CONFIG -> navigateBack()
            SetupStep.INCOMING_CONFIG -> changeToSetupStep(SetupStep.AUTO_CONFIG)
            SetupStep.INCOMING_VALIDATION -> {
                if (state.value.isAutomaticConfig) {
                    changeToSetupStep(SetupStep.AUTO_CONFIG)
                } else {
                    changeToSetupStep(SetupStep.INCOMING_CONFIG)
                }
            }

            SetupStep.OUTGOING_CONFIG -> changeToSetupStep(SetupStep.INCOMING_VALIDATION)
            SetupStep.OUTGOING_VALIDATION -> {
                if (state.value.isAutomaticConfig) {
                    changeToSetupStep(SetupStep.AUTO_CONFIG)
                } else {
                    changeToSetupStep(SetupStep.OUTGOING_CONFIG)
                }
            }

            SetupStep.OPTIONS -> changeToSetupStep(SetupStep.OUTGOING_VALIDATION)
        }
    }

    private fun onNext() {
        when (state.value.setupStep) {
            SetupStep.AUTO_CONFIG -> {
                if (state.value.isAutomaticConfig) {
                    emitEffect(Effect.UpdateIncomingConfigValidation)
                    emitEffect(Effect.UpdateOutgoingConfigValidation)
                    changeToSetupStep(SetupStep.INCOMING_VALIDATION)
                } else {
                    changeToSetupStep(SetupStep.INCOMING_CONFIG)
                }
            }

            SetupStep.INCOMING_CONFIG -> {
                emitEffect(Effect.UpdateIncomingConfigValidation)
                changeToSetupStep(SetupStep.INCOMING_VALIDATION)
            }

            SetupStep.INCOMING_VALIDATION -> {
                if (state.value.isAutomaticConfig) {
                    changeToSetupStep(SetupStep.OUTGOING_VALIDATION)
                } else {
                    changeToSetupStep(SetupStep.OUTGOING_CONFIG)
                }
            }

            SetupStep.OUTGOING_CONFIG -> {
                emitEffect(Effect.UpdateOutgoingConfigValidation)
                changeToSetupStep(SetupStep.OUTGOING_VALIDATION)
            }

            SetupStep.OUTGOING_VALIDATION -> {
                changeToSetupStep(SetupStep.OPTIONS)
            }

            SetupStep.OPTIONS -> onFinish()
        }
    }

    private fun changeToSetupStep(setupStep: SetupStep) {
        updateState {
            it.copy(
                setupStep = setupStep,
            )
        }
    }

    private fun onFinish() {
        emitEffect(Effect.CollectExternalStates)
    }

    private fun onStateCollected(
        autoDiscoveryState: AccountAutoDiscoveryContract.State,
        incomingState: AccountIncomingConfigContract.State,
        outgoingState: AccountOutgoingConfigContract.State,
        optionsState: AccountOptionsContract.State,
    ) {
        viewModelScope.launch {
            val result = createAccount.execute(
                emailAddress = autoDiscoveryState.emailAddress.value,
                incomingServerSettings = incomingState.toServerSettings(),
                outgoingServerSettings = outgoingState.toServerSettings(),
                options = optionsState.toAccountOptions(),
            )

            navigateNext(result)
        }
    }

    private fun navigateNext(accountUuid: String) = emitEffect(Effect.NavigateNext(accountUuid))

    private fun navigateBack() = emitEffect(Effect.NavigateBack)
}
