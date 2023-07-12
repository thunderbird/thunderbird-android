package app.k9mail.feature.account.setup.ui.incoming

import androidx.lifecycle.viewModelScope
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.setup.domain.entity.toDefaultPort
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Effect
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Error
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Event
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.State
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Validator
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.ViewModel
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CONTINUE_NEXT_DELAY = 1000L

@Suppress("TooManyFunctions")
internal class AccountIncomingConfigViewModel(
    initialState: State = State(),
    private val validator: Validator,
    private val checkIncomingServerConfig: UseCase.CheckIncomingServerConfig,
) : BaseViewModel<State, Event, Effect>(initialState), ViewModel {

    override fun initState(state: State) {
        updateState {
            state.copy()
        }
    }

    @Suppress("CyclomaticComplexMethod")
    override fun event(event: Event) {
        when (event) {
            is Event.ProtocolTypeChanged -> updateProtocolType(event.protocolType)
            is Event.ServerChanged -> updateState { it.copy(server = it.server.updateValue(event.server)) }
            is Event.SecurityChanged -> updateSecurity(event.security)
            is Event.PortChanged -> updateState { it.copy(port = it.port.updateValue(event.port)) }
            is Event.AuthenticationTypeChanged -> updateState { it.copy(authenticationType = event.authenticationType) }
            is Event.UsernameChanged -> updateState { it.copy(username = it.username.updateValue(event.username)) }
            is Event.PasswordChanged -> updateState { it.copy(password = it.password.updateValue(event.password)) }
            is Event.ClientCertificateChanged -> updateState { it.copy(clientCertificate = event.clientCertificate) }
            is Event.ImapAutoDetectNamespaceChanged -> updateState {
                it.copy(imapAutodetectNamespaceEnabled = event.enabled)
            }

            is Event.ImapPrefixChanged -> updateState {
                it.copy(imapPrefix = it.imapPrefix.updateValue(event.imapPrefix))
            }

            is Event.ImapUseCompressionChanged -> updateState { it.copy(imapUseCompression = event.useCompression) }
            is Event.ImapSendClientIdChanged -> updateState { it.copy(imapSendClientId = event.sendClientId) }

            Event.OnNextClicked -> onNext()
            Event.OnBackClicked -> onBack()
            Event.OnRetryClicked -> onRetry()
        }
    }

    private fun onNext() {
        if (state.value.isSuccess) {
            navigateNext()
        } else {
            submit()
        }
    }

    private fun updateProtocolType(protocolType: IncomingProtocolType) {
        updateState {
            val allowedAuthenticationTypesForNewProtocol = protocolType.allowedAuthenticationTypes
            val newAuthenticationType = if (it.authenticationType in allowedAuthenticationTypesForNewProtocol) {
                it.authenticationType
            } else {
                allowedAuthenticationTypesForNewProtocol.first()
            }

            it.copy(
                protocolType = protocolType,
                security = protocolType.defaultConnectionSecurity,
                port = it.port.updateValue(
                    protocolType.toDefaultPort(protocolType.defaultConnectionSecurity),
                ),
                authenticationType = newAuthenticationType,
            )
        }
    }

    private fun updateSecurity(security: ConnectionSecurity) {
        updateState {
            it.copy(
                security = security,
                port = it.port.updateValue(it.protocolType.toDefaultPort(security)),
            )
        }
    }

    private fun submit() = with(state.value) {
        val serverResult = validator.validateServer(server.value)
        val portResult = validator.validatePort(port.value)
        val usernameResult = validator.validateUsername(username.value)
        val passwordResult = validator.validatePassword(password.value)
        val imapPrefixResult = validator.validateImapPrefix(imapPrefix.value)

        val hasError = listOf(serverResult, portResult, usernameResult, passwordResult, imapPrefixResult)
            .any { it is ValidationResult.Failure }

        updateState {
            it.copy(
                server = it.server.updateFromValidationResult(serverResult),
                port = it.port.updateFromValidationResult(portResult),
                username = it.username.updateFromValidationResult(usernameResult),
                password = it.password.updateFromValidationResult(passwordResult),
                imapPrefix = it.imapPrefix.updateFromValidationResult(imapPrefixResult),
            )
        }

        if (!hasError) {
            checkSettings()
        }
    }

    private fun checkSettings() {
        viewModelScope.launch {
            updateState {
                it.copy(isLoading = true)
            }

            val result = checkIncomingServerConfig.execute(state.value.protocolType, state.value.toServerSettings())
            when (result) {
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
                    Error.UnknownError(result.exception),
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
        checkSettings()
    }

    private fun navigateBack() = emitEffect(Effect.NavigateBack)

    private fun navigateNext() = emitEffect(Effect.NavigateNext)
}
