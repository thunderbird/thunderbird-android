package app.k9mail.feature.navigation.drawer.ui

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount

interface DrawerContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val currentAccount: DisplayAccount? = null,
        val accounts: List<DisplayAccount> = emptyList(),
        val isLoading: Boolean = false,
    )

    sealed interface Event {
        data object OnRefresh : Event
    }

    sealed interface Effect
}
