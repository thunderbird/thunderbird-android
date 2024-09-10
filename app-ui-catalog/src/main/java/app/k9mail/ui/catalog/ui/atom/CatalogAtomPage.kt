package app.k9mail.ui.catalog.ui.atom

import app.k9mail.ui.catalog.ui.CatalogPage
import kotlinx.collections.immutable.toImmutableList

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
    ;

    override fun toString(): String {
        return displayName
    }

    companion object {
        fun all() = entries.toImmutableList()
    }
}
