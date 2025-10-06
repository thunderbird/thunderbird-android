package net.thunderbird.ui.catalog.ui.page.atom

import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.ui.catalog.ui.page.CatalogPageContract.CatalogPage

enum class CatalogAtomPage(
    override val displayName: String,
    override val isFullScreen: Boolean = false,
) : CatalogPage {
    TYPOGRAPHY("Typography"),
    COLOR("Colors"),
    BUTTON("Buttons"),
    SELECTION_CONTROL("Selection Controls"),
    TEXT_FIELD("TextFields"),
    ICON("Icons"),
    IMAGE("Images"),
    CARD("Cards"),
    ;

    override fun toString(): String {
        return displayName
    }

    companion object {
        fun all() = entries.toImmutableList()
    }
}
