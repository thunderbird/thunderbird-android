package com.fsck.k9.ui.settings.general

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.thunderbird.core.android.logging.LogFileWriter
import timber.log.Timber

class GeneralSettingsViewModel(private val logFileWriter: LogFileWriter) : ViewModel() {
    private var snackbarJob: Job? = null
    private val uiStateFlow = MutableStateFlow<GeneralSettingsUiState>(GeneralSettingsUiState.Idle)
    val uiState: Flow<GeneralSettingsUiState> = uiStateFlow

    fun exportLogs(contentUri: Uri) {
        viewModelScope.launch {
            setExportingState()

            try {
                logFileWriter.writeLogTo(contentUri)
                showSnackbar(GeneralSettingsUiState.Success)
            } catch (e: Exception) {
                Timber.e(e, "Failed to write log to URI: %s", contentUri)
                showSnackbar(GeneralSettingsUiState.Failure)
            }
        }
    }

    private fun setExportingState() {
        // If an export was triggered before and the success/failure Snackbar is still showing, cancel the coroutine
        // that resets the state to Idle after SNACKBAR_DURATION
        snackbarJob?.cancel()
        snackbarJob = null

        sendUiState(GeneralSettingsUiState.Exporting)
    }

    private fun showSnackbar(uiState: GeneralSettingsUiState) {
        snackbarJob?.cancel()
        snackbarJob = viewModelScope.launch {
            sendUiState(uiState)
            delay(SNACKBAR_DURATION)
            sendUiState(GeneralSettingsUiState.Idle)
            snackbarJob = null
        }
    }

    private fun sendUiState(uiState: GeneralSettingsUiState) {
        uiStateFlow.value = uiState
    }

    companion object {
        const val DEFAULT_FILENAME = "k9mail-logs.txt"
        const val SNACKBAR_DURATION = 3000L
    }
}

sealed interface GeneralSettingsUiState {
    val isExportLogsMenuEnabled: Boolean
    val snackbarState: SnackbarState

    object Idle : GeneralSettingsUiState {
        override val isExportLogsMenuEnabled = true
        override val snackbarState = SnackbarState.Hidden
    }

    object Exporting : GeneralSettingsUiState {
        override val isExportLogsMenuEnabled = false
        override val snackbarState = SnackbarState.Hidden
    }

    object Success : GeneralSettingsUiState {
        override val isExportLogsMenuEnabled = true
        override val snackbarState = SnackbarState.ExportLogSuccess
    }

    object Failure : GeneralSettingsUiState {
        override val isExportLogsMenuEnabled = true
        override val snackbarState = SnackbarState.ExportLogFailure
    }
}

enum class SnackbarState {
    Hidden,
    ExportLogSuccess,
    ExportLogFailure,
}
