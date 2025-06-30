package net.thunderbird.feature.navigation.drawer.siderail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.molecule.PullToRefreshBox
import net.thunderbird.feature.navigation.drawer.siderail.FolderDrawerState
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun DrawerView(
    drawerState: FolderDrawerState,
    openAccount: (accountId: String) -> Unit,
    openFolder: (accountId: String, folderId: Long) -> Unit,
    openUnifiedFolder: () -> Unit,
    openManageFolders: () -> Unit,
    openSettings: () -> Unit,
    closeDrawer: () -> Unit,
    viewModel: DrawerContract.ViewModel = koinViewModel<DrawerViewModel>(),
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is DrawerContract.Effect.OpenAccount -> openAccount(effect.accountId)
            is DrawerContract.Effect.OpenFolder -> openFolder(
                effect.accountId,
                effect.folderId,
            )

            DrawerContract.Effect.OpenUnifiedFolder -> openUnifiedFolder()
            is DrawerContract.Effect.OpenManageFolders -> openManageFolders()
            is DrawerContract.Effect.OpenSettings -> openSettings()
            DrawerContract.Effect.CloseDrawer -> closeDrawer()
        }
    }

    LaunchedEffect(drawerState.selectedAccountUuid) {
        dispatch(DrawerContract.Event.SelectAccount(drawerState.selectedAccountUuid))
    }

    LaunchedEffect(drawerState.selectedFolderId) {
        dispatch(DrawerContract.Event.SelectFolder(drawerState.selectedFolderId))
    }

    PullToRefreshBox(
        isRefreshing = state.value.isLoading,
        onRefresh = { dispatch(DrawerContract.Event.OnSyncAccount) },
    ) {
        DrawerContent(
            state = state.value,
            onEvent = { dispatch(it) },
        )
    }
}
