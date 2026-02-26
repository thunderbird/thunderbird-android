package net.thunderbird.ui.catalog.ui.page

import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.ui.catalog.ui.page.atom.CatalogAtomPage

interface CatalogPageContract {

    interface CatalogPage {
        val displayName: String
        val isFullScreen: Boolean
    }

    interface ViewModel : UnidirectionalViewModel<State, Event, Nothing>

    data class State(
        val page: CatalogPage = CatalogAtomPage.TYPOGRAPHY,
    )

    sealed interface Event {
        data class OnPageChanged(
            val page: CatalogPage,
        ) : Event
    }
}
