package com.fsck.k9.ui.changelog

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel

interface ChangelogContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    @Stable
    data class State(
        val releaseItems: ImmutableList<ReleaseUiModel>,
        val showRecentChanges: Boolean,
    )

    sealed interface Event {
        data class OnShowRecentChangesCheck(val isChecked: Boolean) : Event
    }

    sealed interface Effect
}
