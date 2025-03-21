package net.thunderbird.ui.catalog.ui.organism

import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.ui.catalog.ui.CatalogPage

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
