package app.k9mail.feature.navigation.drawer.ui

import androidx.compose.runtime.Stable
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.legacy.ui.folder.DisplayFolder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.immutableListOf
import kotlinx.collections.immutable.persistentListOf

interface DrawerContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    @Stable
    data class State(
        val currentAccount: DisplayAccount? = null,
        val accounts: ImmutableList<DisplayAccount> = persistentListOf(),
        val folders: ImmutableList<DisplayFolder> = persistentListOf(),
        val showStarredCount: Boolean = false,
        val isLoading: Boolean = false,
    )

    sealed interface Event {
        data object OnRefresh : Event
    }

    sealed interface Effect
}
