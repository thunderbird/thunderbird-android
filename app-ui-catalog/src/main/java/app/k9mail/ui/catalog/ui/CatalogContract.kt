package app.k9mail.ui.catalog.ui

interface CatalogContract {

    enum class Theme(
        val displayName: String,
    ) {
        K9("K-9"),
        THUNDERBIRD("Thunderbird"),
    }

    enum class ThemeVariant {
        LIGHT,
        DARK,
    }
}
