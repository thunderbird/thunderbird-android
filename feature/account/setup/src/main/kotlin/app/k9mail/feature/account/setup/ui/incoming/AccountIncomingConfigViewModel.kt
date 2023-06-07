package app.k9mail.feature.account.setup.ui.incoming

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.setup.domain.entity.toDefaultPort
import app.k9mail.feature.account.setup.domain.entity.toDefaultSecurity
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Effect
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Event
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Event.ClientCertificateChanged
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Event.ImapAutoDetectNamespaceChanged
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Event.ImapPrefixChanged
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Event.OnBackClicked
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Event.OnNextClicked
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Event.PasswordChanged
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Event.PortChanged
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Event.ProtocolTypeChanged
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Event.SecurityChanged
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Event.ServerChanged
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Event.UseCompressionChanged
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Event.UsernameChanged
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.State
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.ViewModel

class AccountIncomingConfigViewModel(
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), ViewModel {

    override fun initState(state: State) {
        updateState {
            state.copy()
        }
    }

    override fun event(event: Event) {
        when (event) {
            is ProtocolTypeChanged -> updateProtocolType(event.protocolType)
            is ServerChanged -> updateState { it.copy(server = it.server.updateValue(event.server)) }
            is SecurityChanged -> updateSecurity(event.security)
            is PortChanged -> updateState { it.copy(port = it.port.updateValue(event.port)) }
            is UsernameChanged -> updateState { it.copy(username = it.username.updateValue(event.username)) }
            is PasswordChanged -> updateState { it.copy(password = it.password.updateValue(event.password)) }
            is ClientCertificateChanged -> updateState { it.copy(clientCertificate = event.clientCertificate) }
            is ImapAutoDetectNamespaceChanged -> updateState { it.copy(imapAutodetectNamespaceEnabled = event.enabled) }
            is ImapPrefixChanged -> updateState { it.copy(imapPrefix = it.imapPrefix.updateValue(event.imapPrefix)) }
            is UseCompressionChanged -> updateState { it.copy(useCompression = event.useCompression) }
            OnBackClicked -> navigateBack()
            OnNextClicked -> submit()
        }
    }

    private fun updateProtocolType(protocolType: IncomingProtocolType) {
        updateState {
            it.copy(
                protocolType = protocolType,
                security = protocolType.toDefaultSecurity(),
                port = it.port.updateValue(
                    protocolType.toDefaultPort(protocolType.toDefaultSecurity()),
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

    private fun submit() {
        navigateNext()
    }

    private fun navigateBack() = emitEffect(Effect.NavigateBack)

    private fun navigateNext() = emitEffect(Effect.NavigateNext)
}
