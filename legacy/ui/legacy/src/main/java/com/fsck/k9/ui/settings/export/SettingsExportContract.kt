package com.fsck.k9.ui.settings.export

import android.net.Uri
import androidx.compose.runtime.Stable
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel

interface SettingsExportContract {

    interface ViewModel :
        UnidirectionalViewModel<State,Event,Effect>

    @Stable
    data class State(
        val settingsList: List<SettingsListItem> = emptyList(),
        val isSettingsListEnabled: Boolean = true,
        val exportButtonState: ButtonState = ButtonState.ENABLED,
        val isShareButtonVisible: Boolean = false,
        val isProgressVisible: Boolean = false,
        val statusText: StatusText = StatusText.HIDDEN,
    )

    sealed interface Event {
        data class OnSettingsItemSelectionChange(
            val itemId: String,
            val isSelected: Boolean,
        ) : Event

        data object OnExportClick : Event

        data object OnShareClick : Event

        data class OnDocumentPicked(
            val contentUri: Uri,
        ) : Event

        data object OnDocumentPickCanceled : Event
    }

    sealed interface Effect {
        data class PickDocument(
            val fileNameSuggestion: String,
            val mimeType: String,
        ) : Effect

        data class ShareDocument(
            val contentUri: Uri,
            val mimeType: String,
        ) : Effect
    }
}
