package app.k9mail.ui.catalog.ui.organism

import app.k9mail.ui.catalog.ui.CatalogPage
import kotlinx.collections.immutable.toImmutableList

enum class CatalogOrganismPage(
    override val displayName: String,
    override val isFullScreen: Boolean = false,
) : CatalogPage {
    APP_BAR("App Bars"),
    ;

    override fun toString(): String {
        return displayName
    }

    companion object {
        fun all() = entries.toImmutableList()
    }
}
