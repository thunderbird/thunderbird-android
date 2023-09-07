package app.k9mail.feature.account.server.certificate.ui

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel

interface ServerCertificateErrorContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val errorText: String = "",
    )

    sealed interface Event {
        object OnCertificateAcceptedClicked : Event
        object OnBackClicked : Event
    }

    sealed interface Effect {
        object NavigateCertificateAccepted : Effect
        object NavigateBack : Effect
    }
}
