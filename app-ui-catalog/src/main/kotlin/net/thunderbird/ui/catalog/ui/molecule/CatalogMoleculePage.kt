package net.thunderbird.ui.catalog.ui.molecule

import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.ui.catalog.ui.CatalogPage

enum class CatalogMoleculePage(
    override val displayName: String,
    override val isFullScreen: Boolean = false,
) : CatalogPage {
    INPUT("Inputs"),
    STATE("States"),
    PULL_TO_REFRESH("Pull to refresh", isFullScreen = true),
    ;

    override fun toString(): String {
        return displayName
    }

    companion object {
        fun all() = entries.toImmutableList()
    }
}
