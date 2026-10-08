package net.thunderbird.feature.account.settings.impl.ui.fetchingMail

import androidx.compose.runtime.Stable
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.core.ui.setting.Settings

@Suppress("standard:max-line-length")
interface FetchingMailSettingsContract {
    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    @Stable
    data class State(
        val subtitle: String? = null,
        val localFolderSize: SelectOption,
        val syncMessageFrom: SelectOption,
        val fetchMessageUpTo: SelectOption,
        val folderPollFrequency: SelectOption,
        val syncServerDeletions: Boolean = false,
        val markAsReadWhenDeleted: Boolean = false,
        val whenIDeleteAMessage: SelectOption,
        val eraseDeletedMessageOnServer: SelectOption,
        val maxFolderToCheckWithPush: SelectOption,
        val refreshIdleConnection: SelectOption,
    )

    sealed interface Event {
        data object OnBackPressed : Event
        data class OnLocalFolderSizeChange(val localFolderSize: SelectOption) : Event
        data class OnSyncMessageFromChange(val syncMessageFrom: SelectOption) : Event
        data class OnFetchMessageUpToChange(val fetchMessageUpTo: SelectOption) : Event
        data class OnFolderPollFrequencyChange(val folderPollFrequency: SelectOption) : Event
        data class OnSyncServerDeletionsToggle(
            val syncServerDeletions: Boolean,
        ) : Event

        data class OnMarkAsReadWhenDeletedToggle(
            val markAsReadWhenDeleted: Boolean,
        ) : Event

        data class OnWhenIDeleteAMessageChange(val whenIDeleteAMessage: SelectOption) : Event
        data class OnEraseDeletedMessageOnServerChange(val eraseDeletedMessageOnServer: SelectOption) : Event
        data object OnInComingServerClick : Event
        data object OnAdvanceClick : Event

        data class OnMaxFolderToCheckWithPushChange(val maxFolderToCheckWithPushChanges: SelectOption) : Event
        data class OnRefreshIdleConnectionFrequencyChange(val refreshIdleConnectionFrequency: SelectOption) : Event
    }

    sealed interface Effect {
        object NavigateBack : Effect
        object NavigateToIncomingServerSettings : Effect
        object NavigateToAdvancedFetchingMailSettings : Effect
    }

    interface SettingsBuilder {
        fun buildCoreFetchingMailSettings(
            state: State,
            onEvent: (Event) -> Unit,
        ): Settings

        fun buildAdvancedFetchingMailSettings(
            state: State,
            onEvent: (Event) -> Unit,
        ): Settings
    }
}
