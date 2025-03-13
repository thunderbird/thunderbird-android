package app.k9mail.ui.catalog.ui

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.ui.catalog.ui.CatalogContract.Event
import app.k9mail.ui.catalog.ui.CatalogContract.Event.OnThemeChanged
import app.k9mail.ui.catalog.ui.CatalogContract.Event.OnThemeVariantChanged
import app.k9mail.ui.catalog.ui.CatalogContract.State
import app.k9mail.ui.catalog.ui.CatalogContract.ViewModel

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
