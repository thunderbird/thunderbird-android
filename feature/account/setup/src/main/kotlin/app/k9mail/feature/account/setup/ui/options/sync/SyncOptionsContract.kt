package app.k9mail.feature.account.setup.ui.options.sync

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount

interface SyncOptionsContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val checkFrequency: EmailCheckFrequency = EmailCheckFrequency.DEFAULT,
        val messageDisplayCount: EmailDisplayCount = EmailDisplayCount.DEFAULT,
        val showNotification: Boolean = true,
    )

    sealed interface Event {
        data class OnCheckFrequencyChanged(val checkFrequency: EmailCheckFrequency) : Event
        data class OnMessageDisplayCountChanged(val messageDisplayCount: EmailDisplayCount) : Event
        data class OnShowNotificationChanged(val showNotification: Boolean) : Event

        data object LoadAccountState : Event

        data object OnNextClicked : Event
        data object OnBackClicked : Event
    }

    sealed interface Effect {
        object NavigateNext : Effect
        object NavigateBack : Effect
    }
}
