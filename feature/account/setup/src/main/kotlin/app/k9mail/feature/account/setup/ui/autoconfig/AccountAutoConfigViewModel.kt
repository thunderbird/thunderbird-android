package app.k9mail.feature.account.setup.ui.autoconfig

import androidx.lifecycle.viewModelScope
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.ConfigStep
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.Effect
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.Event
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.State
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.Validator
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.ViewModel
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class AccountAutoConfigViewModel(
    initialState: State = State(),
    private val validator: Validator,
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
            Event.OnNextClicked -> onNext()
            Event.OnBackClicked -> onBack()
            Event.OnRetryClicked -> retry()
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

    private fun onNext() {
        when (state.value.configStep) {
            ConfigStep.EMAIL_ADDRESS -> submitEmail()
            ConfigStep.PASSWORD -> submitPassword()
            ConfigStep.OAUTH -> TODO()
        }
    }

    private fun retry() {
        updateState {
            it.copy(configStep = ConfigStep.EMAIL_ADDRESS)
        }
    }

    private fun submitEmail() {
        viewModelScope.launch {
            val emailValidationResult = validator.validateEmailAddress(state.value.emailAddress.value)
            val hasError = emailValidationResult is ValidationResult.Failure

            updateState {
                it.copy(
                    configStep = if (hasError) ConfigStep.EMAIL_ADDRESS else ConfigStep.PASSWORD,
                    emailAddress = it.emailAddress.updateFromValidationResult(emailValidationResult),
                )
            }
        }
    }

    private fun submitPassword() {
        viewModelScope.launch {
            val emailValidationResult = validator.validateEmailAddress(state.value.emailAddress.value)
            val passwordValidationResult = validator.validatePassword(state.value.password.value)
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
            ConfigStep.EMAIL_ADDRESS -> navigateBack()
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
