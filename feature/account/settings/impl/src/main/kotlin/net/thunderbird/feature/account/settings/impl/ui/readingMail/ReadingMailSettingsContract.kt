package net.thunderbird.feature.account.settings.impl.ui.readingMail

import androidx.compose.runtime.Stable
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.core.ui.setting.Settings

interface ReadingMailSettingsContract {
    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    @Stable
    data class State(
        val subtitle: String? = null,
        val showPictures: SelectOption,
        val isMarkMessageAsReadOnView: Boolean = false,
    )

    sealed interface Event {
        data object OnBackPressed : Event
        data class OnShowPicturesChange(val showPictures: SelectOption) : Event
        data class OnIsMarkMessageAsReadOnViewToggle(val isMarkMessageAsReadOnView: Boolean) : Event
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
