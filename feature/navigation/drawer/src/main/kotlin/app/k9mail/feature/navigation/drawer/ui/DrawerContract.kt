package app.k9mail.feature.navigation.drawer.ui

import androidx.compose.runtime.Stable
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import app.k9mail.feature.navigation.drawer.NavigationDrawerExternalContract.DrawerConfig
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayFolder
import app.k9mail.legacy.account.Account
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal interface DrawerContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    @Stable
    data class State(
        val config: DrawerConfig = DrawerConfig(
            showUnifiedFolders = false,
            showStarredCount = false,
        ),
        val accounts: ImmutableList<DisplayAccount> = persistentListOf(),
        val selectedAccountUuid: String? = null,
        val folders: ImmutableList<DisplayFolder> = persistentListOf(),
        val selectedFolder: DisplayFolder? = null,
        val showAccountSelector: Boolean = false,
        val isLoading: Boolean = false,
    )

    sealed interface Event {
        data class OnAccountClick(val account: DisplayAccount) : Event
        data class OnAccountViewClick(val account: DisplayAccount) : Event
        data class OnFolderClick(val folder: DisplayFolder) : Event
        data object OnAccountSelectorClick : Event
        data object OnManageFoldersClick : Event
        data object OnSettingsClick : Event
        data object OnSyncAccount : Event
        data object OnSyncAllAccounts : Event
    }

    sealed interface Effect {
        data class OpenAccount(val account: Account) : Effect
        data class OpenFolder(val folderId: Long) : Effect
        data object OpenUnifiedFolder : Effect
        data object OpenManageFolders : Effect
        data object OpenSettings : Effect
        data object CloseDrawer : Effect
    }
}
