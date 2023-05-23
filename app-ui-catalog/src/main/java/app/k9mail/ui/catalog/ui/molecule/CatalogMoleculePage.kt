package app.k9mail.ui.catalog.ui.molecule

import kotlinx.collections.immutable.toImmutableList

enum class CatalogMoleculePage(
    private val displayName: String,
) {
    INPUT("Inputs"),
    STATE("States"),
    ;

    override fun toString(): String {
        return displayName
    }

    companion object {
        fun all() = values().toList().toImmutableList()
    }
}
