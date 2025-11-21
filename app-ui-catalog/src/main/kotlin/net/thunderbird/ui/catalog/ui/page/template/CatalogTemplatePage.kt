package net.thunderbird.ui.catalog.ui.page.template

import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.ui.catalog.ui.page.CatalogPageContract.CatalogPage

enum class CatalogTemplatePage(
    override val displayName: String,
    override val isFullScreen: Boolean = false,
) : CatalogPage {
    LAYOUT("Layouts"),
    HORIZONTAL_PAGER("Horizontal Pagers"),
    ;

    override fun toString(): String {
        return displayName
    }

    companion object {
        fun all() = entries.toImmutableList()
    }
}
