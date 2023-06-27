package app.k9mail.feature.account.setup.ui.options

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.Effect
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.Event
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.State
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.Validator
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.ViewModel

internal class AccountOptionsViewModel(
    private val validator: Validator,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), ViewModel {

    override fun initState(state: State) {
        updateState {
            state.copy()
        }
    }

    override fun event(event: Event) {
        when (event) {
            is Event.OnAccountNameChanged -> updateState { state ->
                state.copy(
                    accountName = state.accountName.updateValue(event.accountName),
                )
            }

            is Event.OnDisplayNameChanged -> updateState {
                it.copy(
                    displayName = it.displayName.updateValue(event.displayName),
                )
            }

            is Event.OnEmailSignatureChanged -> updateState {
                it.copy(
                    emailSignature = it.emailSignature.updateValue(event.emailSignature),
                )
            }

            is Event.OnCheckFrequencyChanged -> updateState {
                it.copy(
                    checkFrequency = event.checkFrequency,
                )
            }

            is Event.OnMessageDisplayCountChanged -> updateState { state ->
                state.copy(
                    messageDisplayCount = event.messageDisplayCount,
                )
            }

            is Event.OnShowNotificationChanged -> updateState { state ->
                state.copy(
                    showNotification = event.showNotification,
                )
            }

            Event.OnNextClicked -> submit()
            Event.OnBackClicked -> navigateBack()
        }
    }

    private fun submit() = with(state.value) {
        val accountNameResult = validator.validateAccountName(accountName.value)
        val displayNameResult = validator.validateDisplayName(displayName.value)
        val emailSignatureResult = validator.validateEmailSignature(emailSignature.value)

        val hasError = listOf(
            accountNameResult,
            displayNameResult,
            emailSignatureResult,
        ).any { it is ValidationResult.Failure }

        updateState {
            it.copy(
                accountName = it.accountName.updateFromValidationResult(accountNameResult),
                displayName = it.displayName.updateFromValidationResult(displayNameResult),
                emailSignature = it.emailSignature.updateFromValidationResult(emailSignatureResult),
            )
        }

        if (!hasError) {
            navigateNext()
        }
    }

    private fun navigateBack() = emitEffect(Effect.NavigateBack)

    private fun navigateNext() = emitEffect(Effect.NavigateNext)
}
