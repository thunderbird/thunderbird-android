package app.k9mail.feature.account.setup.ui

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel

interface AccountSetupContract {

    enum class SetupStep {
        AUTO_CONFIG,
        INCOMING_CONFIG,
        OUTGOING_CONFIG,
        OPTIONS,
    }

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val setupStep: SetupStep = SetupStep.AUTO_CONFIG,
    )

    sealed interface Event {
        object OnNext : Event
        object OnBack : Event
    }

    sealed interface Effect {
        object NavigateNext : Effect
        object NavigateBack : Effect
    }
}
