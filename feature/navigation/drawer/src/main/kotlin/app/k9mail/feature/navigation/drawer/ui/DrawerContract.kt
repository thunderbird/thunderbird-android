package app.k9mail.feature.navigation.drawer.ui

import androidx.compose.runtime.Stable
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.legacy.ui.folder.DisplayFolder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

interface DrawerContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    @Stable
    data class State(
        val accounts: ImmutableList<DisplayAccount> = persistentListOf(),
        val currentAccount: DisplayAccount? = null,
        val folders: ImmutableList<DisplayFolder> = persistentListOf(),
        val selectedFolder: DisplayFolder? = null,
        val showStarredCount: Boolean = false,
        val isLoading: Boolean = false,
    )

    sealed interface Event {
        data class OnAccountClick(val account: DisplayAccount) : Event
        data class OnAccountViewClick(val account: DisplayAccount) : Event
        data class OnFolderClick(val folder: DisplayFolder) : Event
        data object OnRefresh : Event
    }

    sealed interface Effect {
        data class OpenFolder(val folderId: Long) : Effect
        data object CloseDrawer : Effect
    }
}
