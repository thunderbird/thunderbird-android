package app.k9mail.ui.catalog.ui

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel

interface CatalogContract {

    enum class Theme(
        val displayName: String,
    ) {
        K9("K-9"),
        THUNDERBIRD("Thunderbird"),
    }

    enum class ThemeVariant(
        val displayName: String,
    ) {
        LIGHT("Light"),
        DARK("Dark"),
    }

    interface ViewModel : UnidirectionalViewModel<State, Event, Nothing>

    data class State(
        val theme: Theme = Theme.K9,
        val themeVariant: ThemeVariant = ThemeVariant.LIGHT,
    )

    sealed interface Event {
        object OnThemeChanged : Event

        object OnThemeVariantChanged : Event
    }
}
