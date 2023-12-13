package app.k9mail.feature.account.setup.ui.createaccount

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.common.domain.AccountDomainContract.AccountStateRepository
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase.CreateAccount
import app.k9mail.feature.account.setup.domain.entity.AccountUuid
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.Effect
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.Event
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.State
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CONTINUE_NEXT_DELAY = 2000L

class CreateAccountViewModel(
    private val createAccount: CreateAccount,
    private val accountStateRepository: AccountStateRepository,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState),
    CreateAccountContract.ViewModel {

    override fun event(event: Event) {
        when (event) {
            Event.CreateAccount -> handleOneTimeEvent(event, ::createAccount)
            Event.OnBackClicked -> maybeNavigateBack()
        }
    }

    private fun createAccount() {
        val accountState = accountStateRepository.getState()

        viewModelScope.launch {
            val result = createAccount.execute(
                emailAddress = accountState.emailAddress ?: "",
                incomingServerSettings = accountState.incomingServerSettings!!,
                outgoingServerSettings = accountState.outgoingServerSettings!!,
                authorizationState = accountState.authorizationState?.state,
                specialFolderSettings = accountState.specialFolderSettings,
                options = accountState.options!!,
            )

            when (result) {
                is AccountCreatorResult.Success -> showSuccess(AccountUuid(result.accountUuid))
                is AccountCreatorResult.Error -> showError(result)
            }
        }
    }

    private fun showSuccess(accountUuid: AccountUuid) {
        updateState {
            it.copy(
                isLoading = false,
                error = null,
            )
        }

        viewModelScope.launch {
            delay(CONTINUE_NEXT_DELAY)
            navigateNext(accountUuid)
        }
    }

    private fun showError(error: AccountCreatorResult.Error) {
        updateState {
            it.copy(
                isLoading = false,
                error = error,
            )
        }
    }

    private fun maybeNavigateBack() {
        if (!state.value.isLoading) {
            navigateBack()
        }
    }

    private fun navigateBack() {
        viewModelScope.coroutineContext.cancelChildren()
        emitEffect(Effect.NavigateBack)
    }

    private fun navigateNext(accountUuid: AccountUuid) {
        viewModelScope.coroutineContext.cancelChildren()
        emitEffect(Effect.NavigateNext(accountUuid))
    }
}
