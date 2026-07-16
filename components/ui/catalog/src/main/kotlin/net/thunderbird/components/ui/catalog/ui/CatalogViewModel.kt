package net.thunderbird.components.ui.catalog.ui

import net.thunderbird.components.ui.catalog.ui.CatalogContract.Event
import net.thunderbird.components.ui.catalog.ui.CatalogContract.Event.OnThemeChanged
import net.thunderbird.components.ui.catalog.ui.CatalogContract.Event.OnThemeVariantChanged
import net.thunderbird.components.ui.catalog.ui.CatalogContract.State
import net.thunderbird.components.ui.catalog.ui.CatalogContract.ViewModel
import net.thunderbird.components.ui.catalog.ui.mvi.BaseViewModel

class CatalogViewModel(
    initialState: State = State(),
) : BaseViewModel<State, Event, Nothing>(initialState), ViewModel {
    override fun event(event: Event) {
        when (event) {
            is OnThemeChanged -> {
                updateState { it.copy(theme = it.theme.next()) }
            }

            is OnThemeVariantChanged -> {
                updateState {
                    it.copy(
                        themeVariant = it.themeVariant.next(),
                    )
                }
            }
        }
    }
}
