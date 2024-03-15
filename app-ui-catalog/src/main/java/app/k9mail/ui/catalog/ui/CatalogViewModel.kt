package app.k9mail.ui.catalog.ui

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.ui.catalog.ui.CatalogContract.Event
import app.k9mail.ui.catalog.ui.CatalogContract.Event.OnThemeChanged
import app.k9mail.ui.catalog.ui.CatalogContract.Event.OnThemeVariantChanged
import app.k9mail.ui.catalog.ui.CatalogContract.State
import app.k9mail.ui.catalog.ui.CatalogContract.Theme
import app.k9mail.ui.catalog.ui.CatalogContract.ThemeVariant
import app.k9mail.ui.catalog.ui.CatalogContract.ViewModel

class CatalogViewModel(
    initialState: State = State(),
) : BaseViewModel<State, Event, Nothing>(initialState), ViewModel {
    override fun event(event: Event) {
        when (event) {
            is OnThemeChanged -> {
                updateState { it.copy(theme = selectNextTheme(it.theme)) }
            }

            is OnThemeVariantChanged -> {
                when (state.value.themeVariant) {
                    ThemeVariant.LIGHT -> updateState { it.copy(themeVariant = ThemeVariant.DARK) }
                    ThemeVariant.DARK -> updateState { it.copy(themeVariant = ThemeVariant.LIGHT) }
                }
            }
        }
    }

    private fun selectNextTheme(currentTheme: Theme): Theme {
        val themes = Theme.entries
        val currentThemeIndex = themes.indexOf(currentTheme)
        val nextThemeIndex = (currentThemeIndex + 1) % themes.size
        return themes[nextThemeIndex]
    }
}
