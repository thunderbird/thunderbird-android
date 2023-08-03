package app.k9mail.feature.account.setup.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationState
import app.k9mail.feature.account.setup.domain.DomainContract
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Effect
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Event
import app.k9mail.feature.account.setup.ui.AccountSetupContract.SetupStep
import app.k9mail.feature.account.setup.ui.AccountSetupContract.State
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract
import app.k9mail.feature.account.setup.ui.autodiscovery.toAccountSetupState
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
class AccountSetupViewModel(
    private val createAccount: UseCase.CreateAccount,
    private val accountSetupStateRepository: DomainContract.AccountSetupStateRepository,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), AccountSetupContract.ViewModel {

    override fun event(event: Event) {
        when (event) {
            is Event.OnAutoDiscoveryFinished -> onAutoDiscoveryFinished(event.state, event.isAutomaticConfig)

            Event.OnBack -> onBack()
            Event.OnNext -> onNext()
        }
    }

    private fun onAutoDiscoveryFinished(
        autoDiscoveryState: AccountAutoDiscoveryContract.State,
        isAutomaticConfig: Boolean,
    ) {
        updateState {
            it.copy(
                isAutomaticConfig = isAutomaticConfig,
            )
        }

        accountSetupStateRepository.save(autoDiscoveryState.toAccountSetupState())

        onNext()
    }

    private fun onNext() {
        when (state.value.setupStep) {
            SetupStep.AUTO_CONFIG -> {
                if (state.value.isAutomaticConfig) {
                    // TODO save state for incoming/outgoing server settings
                    changeToSetupStep(SetupStep.INCOMING_VALIDATION)
                } else {
                    changeToSetupStep(SetupStep.INCOMING_CONFIG)
                }
            }

            SetupStep.INCOMING_CONFIG -> {
                // TODO save state for incoming server settings
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
                // TODO save state for outgoing server settings
                changeToSetupStep(SetupStep.OUTGOING_VALIDATION)
            }

            SetupStep.OUTGOING_VALIDATION -> {
                changeToSetupStep(SetupStep.OPTIONS)
            }

            SetupStep.OPTIONS -> onFinish()
        }
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

            SetupStep.OUTGOING_CONFIG -> changeToSetupStep(SetupStep.INCOMING_CONFIG)
            SetupStep.OUTGOING_VALIDATION -> {
                if (state.value.isAutomaticConfig) {
                    changeToSetupStep(SetupStep.AUTO_CONFIG)
                } else {
                    changeToSetupStep(SetupStep.OUTGOING_CONFIG)
                }
            }

            SetupStep.OPTIONS -> if (state.value.isAutomaticConfig) {
                changeToSetupStep(SetupStep.AUTO_CONFIG)
            } else {
                changeToSetupStep(SetupStep.OUTGOING_CONFIG)
            }
        }
    }

    private fun changeToSetupStep(setupStep: SetupStep) {
        if (setupStep == SetupStep.AUTO_CONFIG) {
            accountSetupStateRepository.saveAuthorizationState(AuthorizationState(null))
        }

        updateState {
            it.copy(
                setupStep = setupStep,
            )
        }
    }

    private fun onFinish() {
        val accountSetupState = accountSetupStateRepository.getState()

        viewModelScope.launch {
            val result = createAccount.execute(
                emailAddress = accountSetupState.emailAddress ?: "",
                incomingServerSettings = accountSetupState.incomingServerSettings!!,
                outgoingServerSettings = accountSetupState.outgoingServerSettings!!,
                authorizationState = accountSetupState.authorizationState?.state,
                options = accountSetupState.options!!,
            )

            navigateNext(result)
        }
    }

    private fun navigateNext(accountUuid: String) = emitEffect(Effect.NavigateNext(accountUuid))

    private fun navigateBack() = emitEffect(Effect.NavigateBack)
}
