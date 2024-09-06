package app.k9mail.feature.navigation.drawer.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Effect
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Event
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.State
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("MagicNumber")
class DrawerViewModel(
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(
    initialState = initialState,
),
    ViewModel {

    override fun event(event: Event) {
        when (event) {
            Event.OnRefresh -> refresh()
        }
    }

    private fun refresh() {
        if (state.value.isLoading) {
            return
        }
        viewModelScope.launch {
            updateState {
                it.copy(isLoading = true)
            }

            // TODO: replace with actual data loading
            delay(500)

            updateState {
                it.copy(isLoading = false)
            }
        }
    }
}
