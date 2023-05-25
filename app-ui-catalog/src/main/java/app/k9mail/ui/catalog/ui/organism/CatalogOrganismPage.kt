package app.k9mail.ui.catalog.ui.organism

import kotlinx.collections.immutable.toImmutableList

enum class CatalogOrganismPage(
    private val displayName: String,
) {
    APP_BAR("App Bars"),
    ;

    override fun toString(): String {
        return displayName
    }

    companion object {
        fun all() = values().toList().toImmutableList()
    }
}
