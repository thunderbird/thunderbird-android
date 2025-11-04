package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.compose.runtime.Stable
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.Settings
import net.thunderbird.core.ui.setting.emptySettings
import net.thunderbird.core.validation.ValidationOutcome

internal interface GeneralSettingsContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    @Stable
    data class State(
        val subtitle: String? = null,
        val settings: Settings = emptySettings(),
    )

    sealed interface Event {
        data class OnSettingValueChange(
            val setting: SettingValue<*>,
        ) : Event

        data object OnBackPressed : Event
    }

    sealed interface Effect {
        object NavigateBack : Effect
    }

    interface Validator {
        fun validateName(name: String): ValidationOutcome
        fun validateMonogram(monogram: String): ValidationOutcome
    }
}
