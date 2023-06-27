package app.k9mail.feature.account.setup.ui.incoming

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.setup.domain.entity.toDefaultPort
import app.k9mail.feature.account.setup.domain.input.NumberInputField
import app.k9mail.feature.account.setup.domain.input.StringInputField
import java.io.IOException
import java.security.cert.X509Certificate

interface AccountIncomingConfigContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect> {
        fun initState(state: State)
    }

    data class State(
        val protocolType: IncomingProtocolType = IncomingProtocolType.DEFAULT,
        val server: StringInputField = StringInputField(),
        val security: ConnectionSecurity = IncomingProtocolType.DEFAULT.defaultConnectionSecurity,
        val port: NumberInputField = NumberInputField(
            IncomingProtocolType.DEFAULT.toDefaultPort(IncomingProtocolType.DEFAULT.defaultConnectionSecurity),
        ),
        val username: StringInputField = StringInputField(),
        val password: StringInputField = StringInputField(),
        val clientCertificate: String = "",
        val imapAutodetectNamespaceEnabled: Boolean = true,
        val imapPrefix: StringInputField = StringInputField(),
        val useCompression: Boolean = true,

        val isSuccess: Boolean = false,
        val error: Error? = null,
        val isLoading: Boolean = false,
    )

    sealed class Event {
        data class ProtocolTypeChanged(val protocolType: IncomingProtocolType) : Event()
        data class ServerChanged(val server: String) : Event()
        data class SecurityChanged(val security: ConnectionSecurity) : Event()
        data class PortChanged(val port: Long?) : Event()
        data class UsernameChanged(val username: String) : Event()
        data class PasswordChanged(val password: String) : Event()
        data class ClientCertificateChanged(val clientCertificate: String) : Event()
        data class ImapAutoDetectNamespaceChanged(val enabled: Boolean) : Event()
        data class ImapPrefixChanged(val imapPrefix: String) : Event()
        data class UseCompressionChanged(val useCompression: Boolean) : Event()

        object OnNextClicked : Event()
        object OnBackClicked : Event()
        object OnRetryClicked : Event()
    }

    sealed class Effect {
        object NavigateNext : Effect()
        object NavigateBack : Effect()
    }

    interface Validator {
        fun validateServer(server: String): ValidationResult
        fun validatePort(port: Long?): ValidationResult
        fun validateUsername(username: String): ValidationResult
        fun validatePassword(password: String): ValidationResult
        fun validateImapPrefix(imapPrefix: String): ValidationResult
    }

    sealed interface Error {
        data class NetworkError(val exception: IOException) : Error
        data class CertificateError(val certificateChain: List<X509Certificate>) : Error
        data class AuthenticationError(val serverMessage: String?) : Error
        data class ServerError(val serverMessage: String?) : Error
        data class UnknownError(val exception: Exception) : Error
    }
}
