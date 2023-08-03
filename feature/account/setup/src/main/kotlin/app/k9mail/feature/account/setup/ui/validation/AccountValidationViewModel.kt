package app.k9mail.feature.account.setup.ui.validation

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.setup.domain.DomainContract
import app.k9mail.feature.account.setup.domain.entity.isOAuth
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.Effect
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.Error
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.Event
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.State
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import app.k9mail.feature.account.oauth.domain.DomainContract as OAuthDomainContract

private const val CONTINUE_NEXT_DELAY = 2000L

@Suppress("TooManyFunctions")
internal class AccountValidationViewModel(
    private val validateServerSettings: DomainContract.UseCase.ValidateServerSettings,
    private val accountSetupStateRepository: DomainContract.AccountSetupStateRepository,
    private val authorizationStateRepository: OAuthDomainContract.AuthorizationStateRepository,
    override val oAuthViewModel: AccountOAuthContract.ViewModel,
    override val isIncomingValidation: Boolean = true,
    initialState: State? = null,
) : BaseViewModel<State, Event, Effect>(
    initialState = initialState ?: accountSetupStateRepository.getState().toValidationState(isIncomingValidation),
),
    AccountValidationContract.ViewModel {

    override fun event(event: Event) {
        when (event) {
            Event.LoadAccountSetupStateAndValidate -> loadAccountSetupStateAndValidate()
            is Event.OnOAuthResult -> onOAuthResult(event.result)
            Event.ValidateServerSettings -> onValidateConfig()
            Event.OnNextClicked -> navigateNext()
            Event.OnBackClicked -> onBack()
            Event.OnRetryClicked -> onRetry()
        }
    }

    private fun loadAccountSetupStateAndValidate() {
        updateState {
            accountSetupStateRepository.getState().toValidationState(isIncomingValidation)
        }
        onValidateConfig()
    }

    private fun onValidateConfig() {
        if (state.value.isSuccess) {
            navigateNext()
        } else if (state.value.serverSettings.isOAuth()) {
            checkOAuthState()
        } else {
            validateServerSettings()
        }
    }

    private fun checkOAuthState() {
        val authorizationState = accountSetupStateRepository.getState().authorizationState
        if (authorizationState != null) {
            if (authorizationStateRepository.isAuthorized(authorizationState)) {
                validateServerSettings()
            } else {
                startOAuthSignIn()
            }
        } else {
            startOAuthSignIn()
        }
    }

    private fun startOAuthSignIn() {
        val hostname = state.value.serverSettings?.host
        val emailAddress = state.value.emailAddress

        if (hostname == null || emailAddress == null) {
            updateError(Error.UnknownError("Hostname or email address not set"))
            return
        } else {
            updateState { state ->
                state.copy(
                    needsAuthorization = true,
                )
            }

            oAuthViewModel.initState(
                AccountOAuthContract.State(
                    hostname = hostname,
                    emailAddress = emailAddress,
                ),
            )
        }
    }

    private fun onOAuthResult(result: OAuthResult) {
        if (result is OAuthResult.Success) {
            accountSetupStateRepository.saveAuthorizationState(result.authorizationState)
            updateState {
                it.copy(
                    needsAuthorization = false,
                )
            }

            validateServerSettings()
        }
    }

    private fun validateServerSettings() {
        viewModelScope.launch {
            val serverSettings = state.value.serverSettings
            if (serverSettings == null) {
                updateError(Error.UnknownError("Server settings not set"))
                return@launch
            }

            updateState {
                it.copy(isLoading = true)
            }

            when (val result = validateServerSettings.execute(serverSettings)) {
                ServerSettingsValidationResult.Success -> updateSuccess()

                is ServerSettingsValidationResult.AuthenticationError -> updateError(
                    Error.AuthenticationError(result.serverMessage),
                )

                is ServerSettingsValidationResult.CertificateError -> updateError(
                    Error.CertificateError(result.certificateChain),
                )

                is ServerSettingsValidationResult.NetworkError -> updateError(
                    Error.NetworkError(result.exception),
                )

                is ServerSettingsValidationResult.ServerError -> updateError(
                    Error.ServerError(result.serverMessage),
                )

                is ServerSettingsValidationResult.UnknownError -> updateError(
                    Error.UnknownError(result.exception.message ?: "Unknown error"),
                )
            }
        }
    }

    private fun updateSuccess() {
        updateState {
            it.copy(
                isLoading = false,
                isSuccess = true,
            )
        }

        viewModelScope.launch {
            delay(CONTINUE_NEXT_DELAY)
            navigateNext()
        }
    }

    private fun updateError(error: Error) {
        updateState {
            it.copy(
                error = error,
                isLoading = false,
                isSuccess = false,
            )
        }
    }

    private fun onBack() {
        navigateBack()
    }

    private fun onRetry() {
        updateState {
            it.copy(
                error = null,
            )
        }
        onValidateConfig()
    }

    private fun navigateBack() {
        viewModelScope.coroutineContext.cancelChildren()
        emitEffect(Effect.NavigateBack)
    }

    private fun navigateNext() {
        viewModelScope.coroutineContext.cancelChildren()
        emitEffect(Effect.NavigateNext)
    }
}
