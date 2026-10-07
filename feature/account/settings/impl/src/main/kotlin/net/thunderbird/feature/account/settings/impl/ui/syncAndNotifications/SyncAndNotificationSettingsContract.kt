package net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications

import androidx.compose.runtime.Stable
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.Settings

/* TODO #11683
 *   This view will eventually have one option, but for #11682,
 *   it will only hold navigation to other settings views
 */
interface SyncAndNotificationSettingsContract {

    interface ViewModel: UnidirectionalViewModel<State, Event, Effect>

    @Stable
    data class State(
        val enablePushNotifications: Boolean, // TODO #11683
    )

    sealed interface Event {
        data object OnBackPressed : Event
        data class OnEnablePushNotificationChange(val enablePushNotifications: SettingValue.Select.SelectOption) : Event
        data object OnSelectNotificationStyle : Event
        data object OnSelectFolderSyncAndPush : Event
        data object OnSelectFetchingAndDataLimits : Event
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
