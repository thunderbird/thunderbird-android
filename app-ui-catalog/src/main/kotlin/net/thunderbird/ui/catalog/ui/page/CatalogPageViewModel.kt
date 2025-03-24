package net.thunderbird.ui.catalog.ui.page

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import net.thunderbird.ui.catalog.ui.page.CatalogPageContract.Event
import net.thunderbird.ui.catalog.ui.page.CatalogPageContract.State
import net.thunderbird.ui.catalog.ui.page.CatalogPageContract.ViewModel

class CatalogPageViewModel(
    initialState: State = State(),
) : BaseViewModel<State, Event, Nothing>(initialState), ViewModel {

    override fun event(event: Event) {
        when (event) {
            is Event.OnPageChanged -> {
                updateState {
                    it.copy(
                        page = event.page,
                    )
                }
            }
        }
    }
}
