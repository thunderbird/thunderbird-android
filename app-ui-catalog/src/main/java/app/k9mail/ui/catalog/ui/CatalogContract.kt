package app.k9mail.ui.catalog.ui

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel

interface CatalogContract {

    enum class Theme(
        val displayName: String,
    ) {
        K9("K-9"),
        THUNDERBIRD("Thunderbird"),

        // Theme 2
        THEME_2_K9("K-9 (Material3)"),
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
        data object OnThemeChanged : Event

        data object OnThemeVariantChanged : Event
    }
}

fun CatalogContract.Theme.next(): CatalogContract.Theme {
    val themes = CatalogContract.Theme.entries
    val currentThemeIndex = themes.indexOf(this)
    val nextThemeIndex = (currentThemeIndex + 1) % themes.size
    return themes[nextThemeIndex]
}

fun CatalogContract.ThemeVariant.next(): CatalogContract.ThemeVariant {
    val variants = CatalogContract.ThemeVariant.entries
    val currentVariantIndex = variants.indexOf(this)
    val nextVariantIndex = (currentVariantIndex + 1) % variants.size
    return variants[nextVariantIndex]
}
