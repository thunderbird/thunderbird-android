package app.k9mail.feature.account.server.settings.ui.incoming

import app.k9mail.feature.account.common.domain.entity.AuthenticationType
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.common.domain.entity.toDefaultPort
import app.k9mail.feature.account.common.ui.WithInteractionMode
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.input.NumberInputField
import net.thunderbird.core.validation.input.StringInputField

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
        val isGraphReauthorizationAvailable: Boolean = false,
        val apiOAuthClientId: StringInputField = StringInputField(),
        val apiOAuthRedirectUri: StringInputField = StringInputField(),
        val apiConfigurationError: Boolean = false,
        val preserveLegacyGraphProtocol: Boolean = false,
        val zohoClientId: StringInputField = StringInputField(),
        val zohoRedirectUri: StringInputField = StringInputField(),
        val zohoIncomingOnly: Boolean = false,
        val zohoConfigurationError: Boolean = false,
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
        data class ApiOAuthClientIdChanged(val clientId: String) : Event
        data class ApiOAuthRedirectUriChanged(val redirectUri: String) : Event
        data class ZohoClientIdChanged(val clientId: String) : Event
        data class ZohoRedirectUriChanged(val redirectUri: String) : Event
        data class ZohoIncomingOnlyChanged(val enabled: Boolean) : Event
        data object ReauthorizeZohoAccountClicked : Event
        data object ReauthorizeMicrosoftAccountClicked : Event

        data object LoadAccountState : Event

        data object OnNextClicked : Event
        data object OnBackClicked : Event
    }

    sealed interface Effect {
        data object NavigateNext : Effect
        data object NavigateBack : Effect
        data object ReauthorizeMicrosoftAccount : Effect
    }

    interface Validator {
        fun validateServer(server: String): ValidationOutcome
        fun validatePort(port: Long?): ValidationOutcome
        fun validateUsername(username: String): ValidationOutcome
        fun validatePassword(password: String): ValidationOutcome
        fun validateImapPrefix(imapPrefix: String): ValidationOutcome
    }
}
