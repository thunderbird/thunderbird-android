package app.k9mail.ui.catalog.ui.atom

import kotlinx.collections.immutable.toImmutableList

enum class CatalogAtomPage(
    private val displayName: String,
) {
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
        fun all() = values().toList().toImmutableList()
    }
}
