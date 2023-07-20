package app.k9mail.feature.account.setup.ui.validation

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.setup.domain.DomainContract
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.Effect
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.Error
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.Event
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.State
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.ViewModel
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CONTINUE_NEXT_DELAY = 1000L

internal class AccountValidationViewModel(
    initialState: State = State(),
    private val validateServerSettings: DomainContract.UseCase.ValidateServerSettings,
) : BaseViewModel<State, Event, Effect>(initialState), ViewModel {

    override fun initState(state: State) {
        updateState {
            state.copy(
                isLoading = false,
                error = null,
                isSuccess = false,
            )
        }
    }

    override fun event(event: Event) {
        when (event) {
            Event.ValidateServerSettings -> onValidateConfig()
            Event.OnNextClicked -> TODO()
            Event.OnBackClicked -> onBack()
            Event.OnRetryClicked -> onRetry()
        }
    }

    private fun onValidateConfig() {
        if (state.value.isSuccess) {
            navigateNext()
        } else {
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
        if (state.value.isSuccess) {
            updateState {
                it.copy(
                    isSuccess = false,
                )
            }
        } else if (state.value.error != null) {
            updateState {
                it.copy(
                    error = null,
                )
            }
        } else {
            navigateBack()
        }
    }

    private fun onRetry() {
        updateState {
            it.copy(
                error = null,
            )
        }
        onValidateConfig()
    }

    private fun navigateBack() = emitEffect(Effect.NavigateBack)

    private fun navigateNext() {
        emitEffect(Effect.NavigateNext)
    }
}
