package app.k9mail.feature.account.setup.ui.options.display

import androidx.lifecycle.viewModelScope
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.setup.AccountSetupExternalContract
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.Effect
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.Event
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.State
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.Validator
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.ViewModel
import kotlinx.coroutines.launch

internal class DisplayOptionsViewModel(
    private val validator: Validator,
    private val accountStateRepository: AccountDomainContract.AccountStateRepository,
    private val accountOwnerNameProvider: AccountSetupExternalContract.AccountOwnerNameProvider,
    initialState: State? = null,
) : BaseViewModel<State, Event, Effect>(
    initialState = initialState ?: accountStateRepository.getState().toDisplayOptionsState(),
),
    ViewModel {

    override fun event(event: Event) {
        when (event) {
            Event.LoadAccountState -> handleOneTimeEvent(event, ::loadAccountState)

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

            Event.OnNextClicked -> submit()
            Event.OnBackClicked -> navigateBack()
        }
    }

    private fun loadAccountState() {
        viewModelScope.launch {
            val ownerName = accountOwnerNameProvider.getOwnerName().orEmpty()

            updateState {
                val displayOptionsState = accountStateRepository.getState().toDisplayOptionsState()
                if (displayOptionsState.displayName.value.isEmpty()) {
                    displayOptionsState.copy(
                        displayName = StringInputField(value = ownerName),
                    )
                } else {
                    displayOptionsState
                }
            }
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
            accountStateRepository.setDisplayOptions(state.value.toAccountDisplayOptions())
            navigateNext()
        }
    }

    private fun navigateBack() = emitEffect(Effect.NavigateBack)

    private fun navigateNext() = emitEffect(Effect.NavigateNext)
}
