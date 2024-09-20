package app.k9mail.feature.navigation.drawer.ui

import androidx.compose.runtime.Stable
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccountFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DrawerConfig
import app.k9mail.legacy.account.Account
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

interface DrawerContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    @Stable
    data class State(
        val config: DrawerConfig = DrawerConfig(
            showUnifiedFolders = false,
            showStarredCount = false,
        ),
        val accounts: ImmutableList<DisplayAccount> = persistentListOf(),
        val selectedAccount: DisplayAccount? = null,
        val folders: ImmutableList<DisplayAccountFolder> = persistentListOf(),
        val selectedFolder: DisplayAccountFolder? = null,
        val showAccountSelector: Boolean = false,
        val isLoading: Boolean = false,
    )

    sealed interface Event {
        data class OnAccountClick(val account: DisplayAccount) : Event
        data class OnAccountViewClick(val account: DisplayAccount) : Event
        data class OnFolderClick(val folder: DisplayAccountFolder) : Event
        data object OnAccountSelectorClick : Event
        data object OnManageFoldersClick : Event
        data object OnSettingsClick : Event
        data object OnRefresh : Event
    }

    sealed interface Effect {
        data class OpenAccount(val account: Account) : Effect
        data class OpenFolder(val folderId: Long) : Effect
        data object OpenManageFolders : Effect
        data object OpenSettings : Effect
        data object CloseDrawer : Effect
    }
}
