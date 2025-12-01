package app.k9mail.feature.account.server.settings.ui.incoming

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.common.domain.entity.toDefaultPort
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Effect
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Validator
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.ViewModel
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationSuccess

open class IncomingServerSettingsViewModel(
    initialState: State = State(),
    override val mode: InteractionMode,
    private val validator: Validator,
    private val accountStateRepository: AccountDomainContract.AccountStateRepository,
) : BaseViewModel<State, Event, Effect>(initialState = initialState), ViewModel {

    @Suppress("CyclomaticComplexMethod")
    override fun event(event: Event) {
        when (event) {
            Event.LoadAccountState -> handleOneTimeEvent(event, ::loadAccountState)

            is Event.ProtocolTypeChanged -> updateProtocolType(event.protocolType)
            is Event.ServerChanged -> updateState { it.copy(server = it.server.updateValue(event.server)) }
            is Event.SecurityChanged -> updateSecurity(event.security)
            is Event.PortChanged -> updateState { it.copy(port = it.port.updateValue(event.port)) }
            is Event.AuthenticationTypeChanged -> updateState { it.copy(authenticationType = event.authenticationType) }
            is Event.UsernameChanged -> updateState { it.copy(username = it.username.updateValue(event.username)) }
            is Event.PasswordChanged -> updateState { it.copy(password = it.password.updateValue(event.password)) }
            is Event.ClientCertificateChanged -> updateState {
                it.copy(clientCertificateAlias = event.clientCertificateAlias)
            }

            is Event.ImapAutoDetectNamespaceChanged -> updateState {
                it.copy(imapAutodetectNamespaceEnabled = event.enabled)
            }

            is Event.ImapPrefixChanged -> updateState {
                it.copy(imapPrefix = it.imapPrefix.updateValue(event.imapPrefix))
            }

            is Event.ImapUseCompressionChanged -> updateState { it.copy(imapUseCompression = event.useCompression) }
            is Event.ImapSendClientInfoChanged -> updateState { it.copy(imapSendClientInfo = event.sendClientInfo) }

            Event.OnNextClicked -> onNext()
            Event.OnBackClicked -> onBack()
        }
    }

    protected open fun loadAccountState() {
        updateState {
            accountStateRepository.getState().toIncomingServerSettingsState()
        }
    }

    private fun onNext() {
        submitConfig()
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

    private fun submitConfig() = with(state.value) {
        val serverResult = validator.validateServer(server.value)
        val portResult = validator.validatePort(port.value)
        val usernameResult = validator.validateUsername(username.value)
        val passwordResult = if (authenticationType.isPasswordRequired) {
            validator.validatePassword(password.value)
        } else {
            ValidationSuccess
        }
        val imapPrefixResult = validator.validateImapPrefix(imapPrefix.value)

        val hasError = listOf(serverResult, portResult, usernameResult, passwordResult, imapPrefixResult)
            .any { it is Outcome.Failure }

        updateState {
            it.copy(
                server = it.server.updateFromValidationOutcome(serverResult),
                port = it.port.updateFromValidationOutcome(portResult),
                username = it.username.updateFromValidationOutcome(usernameResult),
                password = it.password.updateFromValidationOutcome(passwordResult),
                imapPrefix = it.imapPrefix.updateFromValidationOutcome(imapPrefixResult),
            )
        }

        if (!hasError) {
            accountStateRepository.setIncomingServerSettings(state.value.toServerSettings())
            navigateNext()
        }
    }

    private fun onBack() {
        navigateBack()
    }

    private fun navigateBack() = emitEffect(Effect.NavigateBack)

    private fun navigateNext() = emitEffect(Effect.NavigateNext)
}
