package net.thunderbird.feature.mail.message.list.api.ui.dialog

import androidx.compose.runtime.Stable
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import net.thunderbird.feature.mail.folder.api.RemoteFolder

sealed interface SetupArchiveFolderDialogContract {
    abstract class ViewModel(
        initialState: State,
    ) : BaseViewModel<State, Event, Effect>(initialState), UnidirectionalViewModel<State, Event, Effect>

    sealed interface State {
        val isDoNotShowDialogAgainChecked: Boolean

        data class EmailCantBeArchived(override val isDoNotShowDialogAgainChecked: Boolean = false) : State
        data class Closed(override val isDoNotShowDialogAgainChecked: Boolean = false) : State

        @Stable
        data class ChooseArchiveFolder(
            val isLoadingFolders: Boolean,
            override val isDoNotShowDialogAgainChecked: Boolean = false,
            val folders: List<RemoteFolder> = emptyList(),
            val selectedFolder: RemoteFolder? = folders.firstOrNull(),
            val errorMessage: String? = null,
        ) : State

        data class CreateArchiveFolder(
            val folderName: String,
            override val isDoNotShowDialogAgainChecked: Boolean = false,
            val syncingMessage: String? = null,
            val errorMessage: String? = null,
        ) : State
    }

    sealed interface Event {
        data object MoveNext : Event
        data object OnDoneClicked : Event
        data object OnDismissClicked : Event
        data class OnFolderSelected(val folder: RemoteFolder) : Event
        data class OnCreateFolderClicked(val newFolderName: String) : Event
        data class OnDoNotShowDialogAgainChanged(val isChecked: Boolean) : Event
    }

    sealed interface Effect {
        data object DismissDialog : Effect
    }
}
