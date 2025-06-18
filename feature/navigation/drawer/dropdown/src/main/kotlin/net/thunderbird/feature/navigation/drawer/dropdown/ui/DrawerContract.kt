package net.thunderbird.feature.navigation.drawer.dropdown.ui

import androidx.compose.runtime.Stable
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfig
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayTreeFolder

internal interface DrawerContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    @Stable
    data class State(
        val config: DrawerConfig = DrawerConfig(
            showUnifiedFolders = false,
            showStarredCount = false,
            showAccountSelector = true,
        ),
        val accounts: ImmutableList<DisplayAccount> = persistentListOf(),
        val selectedAccountId: String? = null,
        val rootFolder: DisplayTreeFolder = DisplayTreeFolder(
            displayFolder = null,
            displayName = null,
            totalUnreadCount = 0,
            totalStarredCount = 0,
            children = persistentListOf(),
        ),
        val folders: ImmutableList<DisplayFolder> = persistentListOf(),
        val selectedFolderId: String? = null,
        val selectedFolder: DisplayFolder? = null,
        val isLoading: Boolean = false,
    )

    sealed interface Event {
        data class SelectAccount(val accountId: String?) : Event
        data class SelectFolder(val folderId: String?) : Event
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
        data class OpenAccount(val accountId: String) : Effect
        data class OpenFolder(val accountId: String, val folderId: Long) : Effect
        data object OpenUnifiedFolder : Effect
        data object OpenManageFolders : Effect
        data object OpenSettings : Effect
        data object CloseDrawer : Effect
    }
}
