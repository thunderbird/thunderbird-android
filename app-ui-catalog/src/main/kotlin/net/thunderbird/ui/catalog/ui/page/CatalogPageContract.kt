package net.thunderbird.ui.catalog.ui.page

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
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
