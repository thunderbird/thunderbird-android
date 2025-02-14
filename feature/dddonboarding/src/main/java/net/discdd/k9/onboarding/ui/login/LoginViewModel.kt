package net.discdd.k9.onboarding.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import app.k9mail.feature.account.setup.domain.entity.AccountUuid
import app.k9mail.feature.account.setup.domain.usecase.CreateAccount
import net.discdd.k9.onboarding.repository.AuthRepository
import net.discdd.k9.onboarding.repository.AuthRepository.AuthState
import net.discdd.k9.onboarding.ui.login.LoginContract.State
import net.discdd.k9.onboarding.ui.login.LoginContract.Event
import net.discdd.k9.onboarding.ui.login.LoginContract.Effect
import net.discdd.k9.onboarding.util.CreateAccountConstants
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    initialState: State = State(),
    private val createAccount: CreateAccount,
    private val authRepository: AuthRepository
): ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()
    private val _effectFlow = MutableSharedFlow<Effect>(replay = 1)
    val effectFlow: SharedFlow<Effect> = _effectFlow.asSharedFlow()

    fun event(event: Event) {
        when (event){
            is Event.EmailAddressChanged -> setEmailAddress(event.emailAddress)
            is Event.PasswordChanged -> setPassword(event.password)
            is Event.OnClickLogin -> login(email=event.emailAddress, password = event.password)
            Event.CheckAuthState  -> checkAuthState()
        }
    }

    private fun checkAuthState() {
        val (state, ackAdu) = authRepository.getState()
        if (state == AuthState.PENDING){
            navigatePending()
        } else if (state == AuthState.LOGGED_IN && ackAdu != null) {
            createAccount(ackAdu)
        } else if (state == AuthState.LOGGED_OUT && ackAdu != null) {
            // display error on screen
        }
    }

    private fun createAccount(ackAdu: net.discdd.k9.onboarding.model.AcknowledgementAdu) {
        val accountState = AccountState(
            emailAddress = ackAdu.email,
            incomingServerSettings = CreateAccountConstants.INCOMING_SERVER_SETTINGS,
            outgoingServerSettings = CreateAccountConstants.OUTGOING_SERVER_SETTINGS,
            specialFolderSettings = CreateAccountConstants.SPECIAL_FOLDER_SETTINGS,
            displayOptions = CreateAccountConstants.DISPLAY_OPTIONS,
            syncOptions = CreateAccountConstants.SYNC_OPTIONS
        )

        viewModelScope.launch {
            when (val result = createAccount.execute(accountState)) {
                is AccountCreatorResult.Success -> showSuccess(AccountUuid(result.accountUuid))
                is AccountCreatorResult.Error -> showError(result)
            }
        }
    }

    private fun showSuccess(accountUuid: AccountUuid) {
        viewModelScope.launch {
            navigateLoggedIn(accountUuid)
        }
    }

    private fun showError(error: AccountCreatorResult.Error) {
    }

    private fun setEmailAddress(email: String) {
        _state.update {
            it.copy (
                emailAddress = it.emailAddress.updateValue(email)
            )
        }
    }

    private fun setPassword(password: String) {
        _state.update {
            it.copy(
                password = it.password.updateValue(password)
            )
        }
    }

    private fun login(email: String, password: String) {
        authRepository.insertAdu(net.discdd.k9.onboarding.model.LoginAdu(email = email, password = password))
        checkAuthState()
    }

    private fun navigatePending() {
        Log.d("k9", "navigate pending")
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            _effectFlow.emit(Effect.OnPendingState)
        }
    }

    private fun navigateLoggedIn(accountUuid: AccountUuid) {
        Log.d("k9", "navigate loggedin")
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            _effectFlow.emit(Effect.OnLoggedInState(accountUuid))
        }
    }
}
