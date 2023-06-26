package app.k9mail.feature.account.setup.ui.incoming

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.setup.domain.entity.toDefaultPort
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Effect
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Event
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.State
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Validator
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.ViewModel

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

    override fun event(event: Event) {
        when (event) {
            is Event.ProtocolTypeChanged -> updateProtocolType(event.protocolType)
            is Event.ServerChanged -> updateState { it.copy(server = it.server.updateValue(event.server)) }
            is Event.SecurityChanged -> updateSecurity(event.security)
            is Event.PortChanged -> updateState { it.copy(port = it.port.updateValue(event.port)) }
            is Event.UsernameChanged -> updateState { it.copy(username = it.username.updateValue(event.username)) }
            is Event.PasswordChanged -> updateState { it.copy(password = it.password.updateValue(event.password)) }
            is Event.ClientCertificateChanged -> updateState { it.copy(clientCertificate = event.clientCertificate) }
            is Event.ImapAutoDetectNamespaceChanged -> updateState { it.copy(imapAutodetectNamespaceEnabled = event.enabled) }
            is Event.ImapPrefixChanged -> updateState { it.copy(imapPrefix = it.imapPrefix.updateValue(event.imapPrefix)) }
            is Event.UseCompressionChanged -> updateState { it.copy(useCompression = event.useCompression) }

            Event.OnBackClicked -> navigateBack()
            Event.OnNextClicked -> submit()
        }
    }

    private fun updateProtocolType(protocolType: IncomingProtocolType) {
        updateState {
            it.copy(
                protocolType = protocolType,
                security = protocolType.defaultConnectionSecurity,
                port = it.port.updateValue(
                    protocolType.toDefaultPort(protocolType.defaultConnectionSecurity),
                ),
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
            navigateNext()
        }
    }

    private fun navigateBack() = emitEffect(Effect.NavigateBack)

    private fun navigateNext() = emitEffect(Effect.NavigateNext)
}
