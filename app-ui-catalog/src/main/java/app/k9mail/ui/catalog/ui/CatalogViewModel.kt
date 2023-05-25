package app.k9mail.ui.catalog.ui

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.ui.catalog.ui.CatalogContract.Event
import app.k9mail.ui.catalog.ui.CatalogContract.Event.OnThemeChanged
import app.k9mail.ui.catalog.ui.CatalogContract.Event.OnThemeVariantChanged
import app.k9mail.ui.catalog.ui.CatalogContract.State
import app.k9mail.ui.catalog.ui.CatalogContract.Theme.K9
import app.k9mail.ui.catalog.ui.CatalogContract.Theme.THUNDERBIRD
import app.k9mail.ui.catalog.ui.CatalogContract.ThemeVariant.DARK
import app.k9mail.ui.catalog.ui.CatalogContract.ThemeVariant.LIGHT
import app.k9mail.ui.catalog.ui.CatalogContract.ViewModel

class CatalogViewModel(
    initialState: State = State(),
) : BaseViewModel<State, Event, Nothing>(initialState), ViewModel {
    override fun event(event: Event) {
        when (event) {
            is OnThemeChanged -> {
                when (state.value.theme) {
                    K9 -> updateState { it.copy(theme = THUNDERBIRD) }
                    THUNDERBIRD -> updateState { it.copy(theme = K9) }
                }
            }

            is OnThemeVariantChanged -> {
                when (state.value.themeVariant) {
                    LIGHT -> updateState { it.copy(themeVariant = DARK) }
                    DARK -> updateState { it.copy(themeVariant = LIGHT) }
                }
            }
        }
    }
}
