package net.thunderbird.ui.catalog.ui.page.organism

import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.ui.catalog.ui.page.CatalogPageContract.CatalogPage

enum class CatalogOrganismPage(
    override val displayName: String,
    override val isFullScreen: Boolean = false,
) : CatalogPage {
    APP_BAR("App Bars"),
    DIALOG("Dialogs"),
    ;

    override fun toString(): String {
        return displayName
    }

    companion object {
        fun all() = entries.toImmutableList()
    }
}
