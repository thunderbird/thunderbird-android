package net.thunderbird.components.ui.catalog.ui.page

import net.thunderbird.components.ui.catalog.ui.mvi.BaseViewModel
import net.thunderbird.components.ui.catalog.ui.page.CatalogPageContract.Event
import net.thunderbird.components.ui.catalog.ui.page.CatalogPageContract.State
import net.thunderbird.components.ui.catalog.ui.page.CatalogPageContract.ViewModel

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
