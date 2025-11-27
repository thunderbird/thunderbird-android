package net.thunderbird.feature.navigation.drawer.dropdown.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.k9mail.core.ui.compose.common.mvi.observe
import net.thunderbird.feature.navigation.drawer.dropdown.FolderDrawerState
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.Effect
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.Event
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.ViewModel
import org.koin.androidx.compose.koinViewModel

@Suppress("LongParameterList")
@Composable
internal fun DrawerView(
    drawerState: FolderDrawerState,
    openAccount: (accountId: String) -> Unit,
    openFolder: (accountId: String, folderId: Long) -> Unit,
    openUnifiedFolder: () -> Unit,
    openManageFolders: () -> Unit,
    openSettings: () -> Unit,
    openAddAccount: () -> Unit,
    closeDrawer: () -> Unit,
    viewModel: ViewModel = koinViewModel<DrawerViewModel>(),
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.OpenAccount -> openAccount(effect.accountId)
            is Effect.OpenFolder -> openFolder(
                effect.accountId,
                effect.folderId,
            )

            Effect.OpenUnifiedFolder -> openUnifiedFolder()
            is Effect.OpenManageFolders -> openManageFolders()
            is Effect.OpenSettings -> openSettings()
            Effect.OpenAddAccount -> openAddAccount()
            Effect.CloseDrawer -> closeDrawer()
        }
    }

    LaunchedEffect(drawerState.selectedAccountUuid) {
        dispatch(Event.SelectAccount(drawerState.selectedAccountUuid))
    }

    LaunchedEffect(drawerState.selectedFolderId) {
        dispatch(Event.SelectFolder(drawerState.selectedFolderId))
    }

    DrawerContent(
        state = state.value,
        onEvent = { dispatch(it) },
    )
}
