package net.thunderbird.feature.account.settings.impl.ui.search

import androidx.compose.runtime.Stable
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.core.ui.setting.Settings

interface SearchSettingsContract {
    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    @Stable
    data class State(
        val subtitle: String? = null,
        val serverSearchLimit: SelectOption,
    )

    sealed interface Event {
        data object OnBackPressed : Event
        data class OnServerSearchLimitChange(val serverSearchLimit: Int) : Event
    }

    sealed interface Effect {
        object NavigateBack : Effect
    }

    fun interface SettingsBuilder {
        fun build(
            state: State,
            onEvent: (Event) -> Unit,
        ): Settings
    }
}
