package app.k9mail.feature.account.setup.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Effect
import app.k9mail.feature.account.setup.ui.AccountSetupContract.Event
import app.k9mail.feature.account.setup.ui.AccountSetupContract.SetupStep
import app.k9mail.feature.account.setup.ui.AccountSetupContract.State
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract
import app.k9mail.feature.account.setup.ui.common.mapper.toIncomingConfigState
import app.k9mail.feature.account.setup.ui.common.mapper.toOptionsState
import app.k9mail.feature.account.setup.ui.common.mapper.toOutgoingConfigState
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract
import app.k9mail.feature.account.setup.ui.incoming.toServerSettings
import app.k9mail.feature.account.setup.ui.incoming.toValidationState
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract
import app.k9mail.feature.account.setup.ui.options.toAccountOptions
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract
import app.k9mail.feature.account.setup.ui.outgoing.toServerSettings
import app.k9mail.feature.account.setup.ui.outgoing.toValidationState
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract
import com.fsck.k9.mail.oauth.AuthStateStorage
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
class AccountSetupViewModel(
    private val createAccount: UseCase.CreateAccount,
    override val autoDiscoveryViewModel: AccountAutoDiscoveryContract.ViewModel,
    override val incomingViewModel: AccountIncomingConfigContract.ViewModel,
    override val incomingValidationViewModel: AccountValidationContract.ViewModel,
    override val outgoingViewModel: AccountOutgoingConfigContract.ViewModel,
    override val outgoingValidationViewModel: AccountValidationContract.ViewModel,
    override val optionsViewModel: AccountOptionsContract.ViewModel,
    private val authStateStorage: AuthStateStorage,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), AccountSetupContract.ViewModel {

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

            Event.OnBack -> onBack()
            Event.OnNext -> onNext()
        }
    }

    private fun onAutoDiscoveryFinished(
        autoDiscoveryState: AccountAutoDiscoveryContract.State,
    ) {
        authStateStorage.updateAuthorizationState(autoDiscoveryState.authorizationState?.state)
        incomingViewModel.initState(autoDiscoveryState.toIncomingConfigState())
        outgoingViewModel.initState(autoDiscoveryState.toOutgoingConfigState())
        optionsViewModel.initState(autoDiscoveryState.toOptionsState())
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
                    incomingValidationViewModel.initState(incomingViewModel.state.value.toValidationState())
                    outgoingValidationViewModel.initState(outgoingViewModel.state.value.toValidationState())
                    changeToSetupStep(SetupStep.INCOMING_VALIDATION)
                } else {
                    changeToSetupStep(SetupStep.INCOMING_CONFIG)
                }
            }

            SetupStep.INCOMING_CONFIG -> {
                incomingValidationViewModel.initState(incomingViewModel.state.value.toValidationState())
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
                outgoingValidationViewModel.initState(outgoingViewModel.state.value.toValidationState())
                changeToSetupStep(SetupStep.OUTGOING_VALIDATION)
            }

            SetupStep.OUTGOING_VALIDATION -> {
                changeToSetupStep(SetupStep.OPTIONS)
            }

            SetupStep.OPTIONS -> onFinish()
        }
    }

    private fun changeToSetupStep(setupStep: SetupStep) {
        if (setupStep == SetupStep.AUTO_CONFIG) {
            authStateStorage.updateAuthorizationState(authorizationState = null)
        }

        updateState {
            it.copy(
                setupStep = setupStep,
            )
        }
    }

    private fun onFinish() {
        val autoDiscoveryState = autoDiscoveryViewModel.state.value
        val incomingState = incomingViewModel.state.value
        val outgoingState = outgoingViewModel.state.value
        val optionsState = optionsViewModel.state.value

        viewModelScope.launch {
            val result = createAccount.execute(
                emailAddress = autoDiscoveryState.emailAddress.value,
                incomingServerSettings = incomingState.toServerSettings(),
                outgoingServerSettings = outgoingState.toServerSettings(),
                authorizationState = authStateStorage.getAuthorizationState(),
                options = optionsState.toAccountOptions(),
            )

            navigateNext(result)
        }
    }

    private fun navigateNext(accountUuid: String) = emitEffect(Effect.NavigateNext(accountUuid))

    private fun navigateBack() = emitEffect(Effect.NavigateBack)
}
