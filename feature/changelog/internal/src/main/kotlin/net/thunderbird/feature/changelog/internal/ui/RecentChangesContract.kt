package net.thunderbird.feature.changelog.internal.ui

import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel

interface RecentChangesContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val showRecentChangesHint: Boolean = false,
    )

    sealed interface Event {
        data object OnRecentChangesHintDismissed : Event
    }

    sealed interface Effect
}
