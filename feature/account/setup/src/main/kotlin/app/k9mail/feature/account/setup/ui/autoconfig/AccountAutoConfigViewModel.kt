package app.k9mail.feature.account.setup.ui.autoconfig

import androidx.lifecycle.viewModelScope
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.setup.domain.DomainContract
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.ConfigStep
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.Effect
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.Error
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.Event
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.State
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.Validator
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.ViewModel
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class AccountAutoConfigViewModel(
    initialState: State = State(),
    private val validator: Validator,
    private val getAutoDiscovery: DomainContract.GetAutoDiscoveryUseCase,
) : BaseViewModel<State, Event, Effect>(initialState), ViewModel {

    override fun initState(state: State) {
        updateState {
            state.copy()
        }
    }

    override fun event(event: Event) {
        when (event) {
            is Event.EmailAddressChanged -> changeEmailAddress(event.emailAddress)
            is Event.PasswordChanged -> changePassword(event.password)
            is Event.ConfigurationApprovalChanged -> changeConfigurationApproval(event.confirmed)

            Event.OnNextClicked -> onNext()
            Event.OnBackClicked -> onBack()
            Event.OnRetryClicked -> retry()
            Event.OnEditConfigurationClicked -> navigateNext()
        }
    }

    private fun changeEmailAddress(emailAddress: String) {
        updateState {
            State(
                emailAddress = StringInputField(value = emailAddress),
            )
        }
    }

    private fun changePassword(password: String) {
        updateState {
            it.copy(
                password = it.password.updateValue(password),
            )
        }
    }

    private fun changeConfigurationApproval(approved: Boolean) {
        updateState {
            it.copy(
                configurationApproved = it.configurationApproved.updateValue(approved),
            )
        }
    }

    private fun onNext() {
        when (state.value.configStep) {
            ConfigStep.EMAIL_ADDRESS ->
                if (state.value.error != null) {
                    updateState {
                        it.copy(
                            error = null,
                            configStep = ConfigStep.PASSWORD,
                        )
                    }
                } else {
                    submitEmail()
                }

            ConfigStep.PASSWORD -> submitPassword()
            ConfigStep.OAUTH -> TODO()
        }
    }

    private fun retry() {
        updateState {
            it.copy(error = null)
        }
        loadAutoConfig()
    }

    private fun submitEmail() {
        with(state.value) {
            val emailValidationResult = validator.validateEmailAddress(emailAddress.value)
            val hasError = emailValidationResult is ValidationResult.Failure

            updateState {
                it.copy(
                    emailAddress = it.emailAddress.updateFromValidationResult(emailValidationResult),
                )
            }

            if (!hasError) {
                loadAutoConfig()
            }
        }
    }

    private fun loadAutoConfig() {
        viewModelScope.launch {
            updateState {
                it.copy(
                    isLoading = true,
                )
            }

            val result = getAutoDiscovery.execute(state.value.emailAddress.value)
            when (result) {
                AutoDiscoveryResult.NoUsableSettingsFound -> updateAutoDiscoverySettings(null)
                is AutoDiscoveryResult.Settings -> updateAutoDiscoverySettings(result)
                is AutoDiscoveryResult.NetworkError -> updateError(Error.NetworkError)
                is AutoDiscoveryResult.UnexpectedException -> updateError(Error.UnknownError)
            }
        }
    }

    private fun updateAutoDiscoverySettings(settings: AutoDiscoveryResult.Settings?) {
        updateState {
            it.copy(
                isLoading = false,
                autoDiscoverySettings = settings,
                configStep = ConfigStep.PASSWORD, // TODO use oauth if applicable
            )
        }
    }

    private fun updateError(error: Error) {
        updateState {
            it.copy(
                isLoading = false,
                error = error,
            )
        }
    }

    private fun submitPassword() {
        with(state.value) {
            val emailValidationResult = validator.validateEmailAddress(emailAddress.value)
            val passwordValidationResult = validator.validatePassword(password.value)
            val hasError = listOf(emailValidationResult, passwordValidationResult)
                .any { it is ValidationResult.Failure }

            updateState {
                it.copy(
                    emailAddress = it.emailAddress.updateFromValidationResult(emailValidationResult),
                    password = it.password.updateFromValidationResult(passwordValidationResult),
                )
            }

            if (!hasError) {
                navigateNext()
            }
        }
    }

    private fun onBack() {
        when (state.value.configStep) {
            ConfigStep.EMAIL_ADDRESS -> {
                if (state.value.error != null) {
                    updateState {
                        it.copy(error = null)
                    }
                } else {
                    navigateBack()
                }
            }

            ConfigStep.PASSWORD -> updateState {
                it.copy(
                    configStep = ConfigStep.EMAIL_ADDRESS,
                    password = StringInputField(),
                )
            }

            ConfigStep.OAUTH -> TODO()
        }
    }

    private fun navigateBack() = emitEffect(Effect.NavigateBack)

    private fun navigateNext() = emitEffect(Effect.NavigateNext)
}
