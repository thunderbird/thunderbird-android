package app.k9mail.feature.account.server.settings.ui.incoming

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.feature.account.common.domain.entity.AuthenticationType
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.common.domain.entity.toDefaultPort
import app.k9mail.feature.account.common.domain.input.NumberInputField
import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.common.ui.WithInteractionMode

interface IncomingServerSettingsContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>, WithInteractionMode

    data class State(
        val protocolType: IncomingProtocolType = IncomingProtocolType.DEFAULT,
        val server: StringInputField = StringInputField(),
        val security: ConnectionSecurity = IncomingProtocolType.DEFAULT.defaultConnectionSecurity,
        val port: NumberInputField = NumberInputField(
            IncomingProtocolType.DEFAULT.toDefaultPort(IncomingProtocolType.DEFAULT.defaultConnectionSecurity),
        ),
        val authenticationType: AuthenticationType = AuthenticationType.PasswordCleartext,
        val username: StringInputField = StringInputField(),
        val password: StringInputField = StringInputField(),
        val clientCertificateAlias: String? = null,
        val imapAutodetectNamespaceEnabled: Boolean = true,
        val imapPrefix: StringInputField = StringInputField(),
        val imapUseCompression: Boolean = true,
        val imapSendClientInfo: Boolean = true,
    )

    sealed interface Event {
        data class ProtocolTypeChanged(val protocolType: IncomingProtocolType) : Event
        data class ServerChanged(val server: String) : Event
        data class SecurityChanged(val security: ConnectionSecurity) : Event
        data class PortChanged(val port: Long?) : Event
        data class AuthenticationTypeChanged(val authenticationType: AuthenticationType) : Event
        data class UsernameChanged(val username: String) : Event
        data class PasswordChanged(val password: String) : Event
        data class ClientCertificateChanged(val clientCertificateAlias: String?) : Event
        data class ImapAutoDetectNamespaceChanged(val enabled: Boolean) : Event
        data class ImapPrefixChanged(val imapPrefix: String) : Event
        data class ImapUseCompressionChanged(val useCompression: Boolean) : Event
        data class ImapSendClientInfoChanged(val sendClientInfo: Boolean) : Event

        data object LoadAccountState : Event

        data object OnNextClicked : Event
        data object OnBackClicked : Event
    }

    sealed interface Effect {
        data object NavigateNext : Effect
        data object NavigateBack : Effect
    }

    interface Validator {
        fun validateServer(server: String): ValidationResult
        fun validatePort(port: Long?): ValidationResult
        fun validateUsername(username: String): ValidationResult
        fun validatePassword(password: String): ValidationResult
        fun validateImapPrefix(imapPrefix: String): ValidationResult
    }
}
