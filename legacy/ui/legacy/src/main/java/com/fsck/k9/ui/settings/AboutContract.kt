package com.fsck.k9.ui.settings

import androidx.compose.runtime.Stable
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal interface AboutContract {

    interface ViewModel :
        UnidirectionalViewModel<State, Event, Effect>

    @Stable
    data class State(
        val version: String = "",
        val libraries: ImmutableList<Library> = persistentListOf(),
    )

    sealed interface Event {
        data object OnChangeLogClick : Event
        data class OnSectionContentClick(val url: String) : Event
        data class OnLibraryClick(val library: Library) : Event
    }

    sealed interface Effect {
        data class OpenUrl(val url: String) : Effect
        data object OpenChangeLog : Effect
    }
}
