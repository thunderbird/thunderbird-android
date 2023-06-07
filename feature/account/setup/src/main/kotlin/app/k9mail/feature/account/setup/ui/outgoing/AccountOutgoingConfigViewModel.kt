package app.k9mail.feature.account.setup.ui.outgoing

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.toSmtpDefaultPort
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Effect
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Event
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Event.ClientCertificateChanged
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Event.ImapAutoDetectNamespaceChanged
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Event.OnBackClicked
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Event.OnNextClicked
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Event.PasswordChanged
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Event.PortChanged
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Event.SecurityChanged
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Event.ServerChanged
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Event.UseCompressionChanged
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Event.UsernameChanged
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.State
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Validator
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.ViewModel

class AccountOutgoingConfigViewModel(
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
            is ServerChanged -> updateState { it.copy(server = it.server.copy(value = event.server)) }
            is SecurityChanged -> updateSecurity(event.security)
            is PortChanged -> updateState { it.copy(port = it.port.copy(value = event.port)) }
            is UsernameChanged -> updateState { it.copy(username = it.username.copy(value = event.username)) }
            is PasswordChanged -> updateState { it.copy(password = it.password.copy(value = event.password)) }
            is ClientCertificateChanged -> updateState { it.copy(clientCertificate = event.clientCertificate) }
            is ImapAutoDetectNamespaceChanged -> updateState { it.copy(imapAutodetectNamespaceEnabled = event.enabled) }
            is UseCompressionChanged -> updateState { it.copy(useCompression = event.useCompression) }

            OnBackClicked -> navigateBack()
            OnNextClicked -> submit()
        }
    }

    private fun updateSecurity(security: ConnectionSecurity) {
        updateState {
            it.copy(
                security = security,
                port = it.port.copy(
                    value = security.toSmtpDefaultPort(),
                    error = null,
                ),
            )
        }
    }

    private fun submit() = with(state.value) {
        val serverResult = validator.validateServer(server.value)
        val portResult = validator.validatePort(port.value)
        val usernameResult = validator.validateUsername(username.value)
        val passwordResult = validator.validatePassword(password.value)

        val hasError = listOf(serverResult, portResult, usernameResult, passwordResult)
            .any { it is ValidationResult.Failure }

        updateState {
            it.copy(
                server = it.server.updateFromValidationResult(serverResult),
                port = it.port.updateFromValidationResult(portResult),
                username = it.username.updateFromValidationResult(usernameResult),
                password = it.password.updateFromValidationResult(passwordResult),
            )
        }

        if (!hasError) {
            navigateNext()
        }
    }

    private fun navigateBack() = emitEffect(Effect.NavigateBack)

    private fun navigateNext() = emitEffect(Effect.NavigateNext)
}
