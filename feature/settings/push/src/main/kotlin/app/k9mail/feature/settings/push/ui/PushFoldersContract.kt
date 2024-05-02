package app.k9mail.feature.settings.push.ui

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import com.fsck.k9.Account.FolderMode

internal interface PushFoldersContract {
    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val isLoading: Boolean = true,
        val showPermissionPrompt: Boolean = false,
        val selectedOption: FolderMode = FolderMode.NONE,
    )

    sealed interface Event {
        data object BackClicked : Event
        data object GrantAlarmPermissionClicked : Event
        data object AlarmPermissionResult : Event
        data class OptionSelected(val option: FolderMode) : Event
    }

    sealed interface Effect {
        data object RequestAlarmPermission : Effect
        data object NavigateBack : Effect
        data class OptionSelected(val option: FolderMode) : Effect
    }
}
