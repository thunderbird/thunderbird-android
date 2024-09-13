package app.k9mail.feature.navigation.drawer.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.molecule.PullToRefreshBox
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Event
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.ViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun DrawerView(
    viewModel: ViewModel = koinViewModel<DrawerViewModel>(),
) {
    val (state, dispatch) = viewModel.observe { }

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
