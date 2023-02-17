package com.fsck.k9.ui.settings.export

class SettingsExportUiModel {
    var settingsList: List<SettingsListItem> = emptyList()
    var isSettingsListEnabled = true
    var exportButton: ButtonState = ButtonState.DISABLED
    var isShareButtonVisible = false
    var isProgressVisible = false
    var statusText = StatusText.HIDDEN

    fun enableExportButton() {
        exportButton = ButtonState.ENABLED
        isShareButtonVisible = false
        isProgressVisible = false
        isSettingsListEnabled = true
    }

    fun disableExportButton() {
        exportButton = ButtonState.DISABLED
        isShareButtonVisible = false
        isProgressVisible = false
    }

    fun showProgress() {
        isProgressVisible = true
        exportButton = ButtonState.INVISIBLE
        isShareButtonVisible = false
        statusText = StatusText.PROGRESS
        isSettingsListEnabled = false
    }

    fun showSuccessText() {
        exportButton = ButtonState.GONE
        isProgressVisible = false
        isShareButtonVisible = true
        isSettingsListEnabled = true
        statusText = StatusText.EXPORT_SUCCESS
    }

    fun showFailureText() {
        exportButton = ButtonState.GONE
        isShareButtonVisible = false
        isProgressVisible = false
        isSettingsListEnabled = true
        statusText = StatusText.EXPORT_FAILURE
    }

    fun initializeSettingsList(list: List<SettingsListItem>) {
        settingsList = list
        updateExportButtonFromSelection()
    }

    fun setSettingsListItemSelection(position: Int, select: Boolean) {
        settingsList[position].selected = select
        statusText = StatusText.HIDDEN
        isShareButtonVisible = false
        updateExportButtonFromSelection()
    }

    private fun updateExportButtonFromSelection() {
        if (isProgressVisible || isShareButtonVisible) return

        val atLeastOnceSelected = settingsList.any { it.selected }
        if (atLeastOnceSelected) {
            enableExportButton()
        } else {
            disableExportButton()
        }
    }
}

sealed class SettingsListItem {
    var selected: Boolean = true

    object GeneralSettings : SettingsListItem()
    data class Account(
        val accountNumber: Int,
        val displayName: String,
        val email: String,
    ) : SettingsListItem()
}

enum class ButtonState {
    DISABLED,
    ENABLED,
    INVISIBLE,
    GONE,
}

enum class StatusText {
    HIDDEN,
    PROGRESS,
    EXPORT_SUCCESS,
    EXPORT_FAILURE,
}
