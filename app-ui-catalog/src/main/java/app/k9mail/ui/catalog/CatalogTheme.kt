package app.k9mail.ui.catalog

enum class CatalogTheme(
    private val displayName: String,
) {
    K9("K-9"),
    THUNDERBIRD("Thunderbird"),
    ;

    override fun toString(): String {
        return displayName
    }
}
