package net.thunderbird.ui.catalog.ui

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import net.thunderbird.ui.catalog.ui.CatalogContract.Event
import net.thunderbird.ui.catalog.ui.CatalogContract.Event.OnThemeChanged
import net.thunderbird.ui.catalog.ui.CatalogContract.Event.OnThemeVariantChanged
import net.thunderbird.ui.catalog.ui.CatalogContract.State
import net.thunderbird.ui.catalog.ui.CatalogContract.ViewModel

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
