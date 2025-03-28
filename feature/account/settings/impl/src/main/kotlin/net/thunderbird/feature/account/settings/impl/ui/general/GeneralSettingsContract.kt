package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.compose.runtime.Stable
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.compose.preference.api.Preference

internal interface GeneralSettingsContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    @Stable
    data class State(
        val subtitle: String? = null,
        val preferences: ImmutableList<Preference> = persistentListOf<Preference>(),
    )

    sealed interface Event {
        data object OnBackPressed : Event
    }

    sealed interface Effect {
        object NavigateBack : Effect
    }
}
