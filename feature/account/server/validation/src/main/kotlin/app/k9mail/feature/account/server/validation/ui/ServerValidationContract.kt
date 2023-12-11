package app.k9mail.feature.account.server.validation.ui

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import com.fsck.k9.mail.ServerSettings
import java.io.IOException
import java.security.cert.X509Certificate

interface ServerValidationContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect> {
        val isIncomingValidation: Boolean
        val oAuthViewModel: AccountOAuthContract.ViewModel
    }

    interface OutgoingViewModel : ViewModel
    interface IncomingViewModel : ViewModel

    data class State(
        val emailAddress: String? = null,
        val serverSettings: ServerSettings? = null,
        val needsAuthorization: Boolean = false,
        val isSuccess: Boolean = false,
        val error: Error? = null,
        val isLoading: Boolean = false,
    )

    sealed interface Event {
        object LoadAccountStateAndValidate : Event

        data class OnOAuthResult(val result: OAuthResult) : Event

        object ValidateServerSettings : Event
        object OnNextClicked : Event
        object OnBackClicked : Event
        object OnRetryClicked : Event
        object OnCertificateAccepted : Event
    }

    sealed interface Effect {
        object NavigateNext : Effect
        object NavigateBack : Effect
    }

    sealed interface Error {
        data class NetworkError(val exception: IOException) : Error
        data class CertificateError(val certificateChain: List<X509Certificate>) : Error
        data object ClientCertificateRetrievalFailure : Error
        data object ClientCertificateExpired : Error
        data class AuthenticationError(val serverMessage: String?) : Error
        data class ServerError(val serverMessage: String?) : Error
        data class MissingServerCapabilityError(val capabilityName: String) : Error
        data class UnknownError(val message: String) : Error
    }
}
