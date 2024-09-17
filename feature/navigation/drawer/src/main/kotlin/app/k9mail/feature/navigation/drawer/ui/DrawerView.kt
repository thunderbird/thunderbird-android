package app.k9mail.feature.navigation.drawer.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.molecule.PullToRefreshBox
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Effect
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Event
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.ViewModel
import app.k9mail.legacy.account.Account
import org.koin.androidx.compose.koinViewModel

@Composable
fun DrawerView(
    openAccount: (account: Account) -> Unit,
    openFolder: (folderId: Long) -> Unit,
    closeDrawer: () -> Unit,
    viewModel: ViewModel = koinViewModel<DrawerViewModel>(),
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.OpenAccount -> openAccount(effect.account)
            is Effect.OpenFolder -> openFolder(effect.folderId)
            Effect.CloseDrawer -> closeDrawer()
        }
    }

    PullToRefreshBox(
        isRefreshing = state.value.isLoading,
        onRefresh = { dispatch(Event.OnRefresh) },
    ) {
        DrawerContent(
            state = state.value,
            onEvent = { dispatch(it) },
        )
    }
}
