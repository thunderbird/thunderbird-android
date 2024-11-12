package app.k9mail.feature.account.edit.ui.server.settings.save

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingErrorState

interface SaveServerSettingsContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect> {
        val isIncoming: Boolean
    }

    data class State(
        override val error: Failure? = null,
        override val isLoading: Boolean = true,
    ) : LoadingErrorState<Failure>

    sealed interface Event {
        data object SaveServerSettings : Event
        data object OnBackClicked : Event
    }

    sealed interface Effect {
        data object NavigateNext : Effect
        data object NavigateBack : Effect
    }

    sealed interface Failure {
        data class SaveServerSettingsFailed(
            val message: String,
        ) : Failure
    }
}
