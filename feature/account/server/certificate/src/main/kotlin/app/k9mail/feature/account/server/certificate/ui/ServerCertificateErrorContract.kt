package app.k9mail.feature.account.server.certificate.ui

import app.k9mail.feature.account.server.certificate.domain.entity.FormattedServerCertificateError
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel

interface ServerCertificateErrorContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val isShowServerCertificate: Boolean = false,
        val certificateError: FormattedServerCertificateError? = null,
    )

    sealed interface Event {
        data object OnShowAdvancedClicked : Event
        data object OnCertificateAcceptedClicked : Event
        data object OnBackClicked : Event
    }

    sealed interface Effect {
        data object NavigateCertificateAccepted : Effect
        data object NavigateBack : Effect
    }
}
