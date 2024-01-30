package app.k9mail.feature.account.server.validation.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.ui.WizardConstants
import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.domain.entity.isOAuth
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.server.certificate.domain.ServerCertificateDomainContract
import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateError
import app.k9mail.feature.account.server.validation.domain.ServerValidationDomainContract
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Effect
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Error
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Event
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.State
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
abstract class BaseServerValidationViewModel(
    private val accountStateRepository: AccountDomainContract.AccountStateRepository,
    private val validateServerSettings: ServerValidationDomainContract.UseCase.ValidateServerSettings,
    private val authorizationStateRepository: AccountOAuthDomainContract.AuthorizationStateRepository,
    private val certificateErrorRepository: ServerCertificateDomainContract.ServerCertificateErrorRepository,
    override val oAuthViewModel: AccountOAuthContract.ViewModel,
    override val isIncomingValidation: Boolean = true,
    initialState: State? = null,
) : BaseViewModel<State, Event, Effect>(
    initialState = initialState ?: accountStateRepository.getState().toServerValidationState(isIncomingValidation),
),
    ServerValidationContract.ViewModel {

    override fun event(event: Event) {
        when (event) {
            Event.LoadAccountStateAndValidate -> handleOneTimeEvent(event, ::loadAccountStateAndValidate)
            is Event.OnOAuthResult -> onOAuthResult(event.result)
            Event.ValidateServerSettings -> onValidateConfig()
            Event.OnNextClicked -> navigateNext()
            Event.OnBackClicked -> onBack()
            Event.OnRetryClicked -> onRetry()
            Event.OnCertificateAccepted -> onRetry()
        }
    }

    private fun loadAccountStateAndValidate() {
        updateState {
            accountStateRepository.getState().toServerValidationState(isIncomingValidation)
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
        val authorizationState = accountStateRepository.getState().authorizationState
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
            accountStateRepository.setAuthorizationState(result.authorizationState)
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

                ServerSettingsValidationResult.ClientCertificateError.ClientCertificateExpired -> updateError(
                    Error.ClientCertificateExpired,
                )

                ServerSettingsValidationResult.ClientCertificateError.ClientCertificateRetrievalFailure -> updateError(
                    Error.ClientCertificateRetrievalFailure,
                )

                is ServerSettingsValidationResult.NetworkError -> updateError(
                    Error.NetworkError(result.exception),
                )

                is ServerSettingsValidationResult.ServerError -> updateError(
                    Error.ServerError(result.serverMessage),
                )

                is ServerSettingsValidationResult.MissingServerCapabilityError -> updateError(
                    Error.MissingServerCapabilityError(result.capabilityName),
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
            delay(WizardConstants.CONTINUE_NEXT_DELAY)
            navigateNext()
        }
    }

    private fun updateError(error: Error) {
        if (error is Error.CertificateError) {
            val serverSettings = checkNotNull(state.value.serverSettings)

            certificateErrorRepository.setCertificateError(
                ServerCertificateError(
                    hostname = serverSettings.host!!,
                    port = serverSettings.port,
                    certificateChain = error.certificateChain,
                ),
            )
        }

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
