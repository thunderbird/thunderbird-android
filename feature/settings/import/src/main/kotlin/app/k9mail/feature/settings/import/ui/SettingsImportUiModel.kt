package app.k9mail.feature.settings.import.ui

import app.k9mail.feature.settings.import.ui.SettingsListItem.GeneralSettings

internal class SettingsImportUiModel {
    var settingsList: List<SettingsListItem> = emptyList()
    var isSettingsListVisible = false
    var isSettingsListEnabled = true
    var importButton: ButtonState = ButtonState.DISABLED
    var closeButton: ButtonState = ButtonState.GONE
    var closeButtonLabel: CloseButtonLabel = CloseButtonLabel.OK
    var isPickDocumentButtonVisible = true
    var isPickDocumentButtonEnabled = true
    var isPickAppButtonVisible = true
    var isPickAppButtonEnabled = false
    var isPickAppButtonPermanentlyDisabled = true
    var isLoadingProgressVisible = false
    var isImportProgressVisible = false
    var statusText = StatusText.HIDDEN

    val hasImportStarted
        get() = importButton == ButtonState.GONE

    val hasDocumentBeenRead
        get() = isSettingsListVisible

    val wasAccountImportSuccessful
        get() = hasImportStarted && settingsList.any { it !is GeneralSettings && it.importStatus.isSuccess }

    fun enablePickButtons() {
        isPickDocumentButtonEnabled = true
        maybeEnablePickAppButton()
    }

    private fun maybeEnablePickAppButton() {
        if (!isPickAppButtonPermanentlyDisabled) {
            isPickAppButtonEnabled = true
        }
    }

    fun disablePickButtons() {
        statusText = StatusText.HIDDEN
        isPickDocumentButtonEnabled = false
        isPickAppButtonEnabled = false
    }

    private fun enableImportButton() {
        importButton = ButtonState.ENABLED
        isImportProgressVisible = false
        isSettingsListEnabled = true
    }

    private fun disableImportButton() {
        importButton = ButtonState.DISABLED
        isImportProgressVisible = false
    }

    fun showLoadingProgress() {
        isLoadingProgressVisible = true
        isPickDocumentButtonVisible = false
        isPickAppButtonVisible = false
        isSettingsListEnabled = false
        statusText = StatusText.HIDDEN
    }

    fun showImportingProgress() {
        isImportProgressVisible = true
        isSettingsListEnabled = false
        importButton = ButtonState.INVISIBLE
        statusText = StatusText.IMPORTING_PROGRESS
    }

    private fun showSuccessText() {
        importButton = ButtonState.GONE
        closeButton = ButtonState.ENABLED
        closeButtonLabel = CloseButtonLabel.OK
        isImportProgressVisible = false
        isSettingsListEnabled = true
        statusText = StatusText.IMPORT_SUCCESS
    }

    private fun showActionRequiredText(actionText: StatusText) {
        importButton = ButtonState.GONE
        closeButton = ButtonState.ENABLED
        closeButtonLabel = CloseButtonLabel.LATER
        isImportProgressVisible = false
        isSettingsListEnabled = true
        statusText = actionText
    }

    fun showReadFailureText() {
        isLoadingProgressVisible = false
        isPickDocumentButtonVisible = true
        isPickDocumentButtonEnabled = true
        isPickAppButtonVisible = true
        maybeEnablePickAppButton()
        statusText = StatusText.IMPORT_READ_FAILURE
        importButton = ButtonState.DISABLED
    }

    fun showImportErrorText() {
        isLoadingProgressVisible = false
        isImportProgressVisible = false
        isSettingsListVisible = false
        isPickDocumentButtonVisible = true
        isPickDocumentButtonEnabled = true
        isPickAppButtonVisible = true
        maybeEnablePickAppButton()
        statusText = StatusText.IMPORT_FAILURE
        importButton = ButtonState.DISABLED
    }

    private fun showPartialImportErrorText() {
        importButton = ButtonState.GONE
        closeButton = ButtonState.ENABLED
        closeButtonLabel = CloseButtonLabel.OK
        isImportProgressVisible = false
        isSettingsListEnabled = true
        statusText = StatusText.IMPORT_PARTIAL_FAILURE
    }

    fun initializeSettingsList(list: List<SettingsListItem>) {
        settingsList = list
        isSettingsListVisible = true
        isLoadingProgressVisible = false
        isPickDocumentButtonVisible = false
        isPickAppButtonVisible = false
        updateImportButtonFromSelection()
    }

    fun toggleSettingsListItemSelection(position: Int) {
        val settingsListItem = settingsList[position]
        settingsListItem.selected = !settingsListItem.selected
        statusText = StatusText.HIDDEN
        updateImportButtonFromSelection()
    }

    fun setSettingsListState(position: Int, status: ImportStatus) {
        settingsList[position].importStatus = status
        settingsList[position].enabled = status.isActionRequired
    }

    private fun updateImportButtonFromSelection() {
        if (isImportProgressVisible) return

        val atLeastOnceSelected = settingsList.any { it.selected }
        if (atLeastOnceSelected) {
            enableImportButton()
        } else {
            disableImportButton()
        }
    }

    fun updateCloseButtonAndImportStatusText() {
        val errorsOnly = settingsList.none { it.importStatus.isSuccess }
        if (errorsOnly) {
            showImportErrorText()
            return
        }

        val passwordsMissing = settingsList.any {
            it.importStatus == ImportStatus.IMPORT_SUCCESS_PASSWORD_REQUIRED
        }
        val authorizationRequired = settingsList.any {
            it.importStatus == ImportStatus.IMPORT_SUCCESS_AUTHORIZATION_REQUIRED
        }

        if (passwordsMissing && authorizationRequired) {
            showActionRequiredText(StatusText.IMPORT_SUCCESS_PASSWORD_AND_AUTHORIZATION_REQUIRED)
        } else if (passwordsMissing) {
            showActionRequiredText(StatusText.IMPORT_SUCCESS_PASSWORD_REQUIRED)
        } else if (authorizationRequired) {
            showActionRequiredText(StatusText.IMPORT_SUCCESS_AUTHORIZATION_REQUIRED)
        } else {
            val partialImportError = settingsList.any { it.importStatus == ImportStatus.IMPORT_FAILURE }
            if (partialImportError) {
                showPartialImportErrorText()
            } else {
                showSuccessText()
            }
        }
    }
}

sealed class SettingsListItem {
    var selected: Boolean = true
    var enabled: Boolean = true
    var importStatus: ImportStatus = ImportStatus.NOT_AVAILABLE

    class GeneralSettings : SettingsListItem()
    class Account(val accountIndex: Int, var displayName: String) : SettingsListItem()
}

enum class ImportStatus(val isSuccess: Boolean, val isActionRequired: Boolean) {
    NOT_AVAILABLE(isSuccess = false, isActionRequired = false),
    NOT_SELECTED(isSuccess = false, isActionRequired = false),
    IMPORT_SUCCESS(isSuccess = true, isActionRequired = false),
    IMPORT_SUCCESS_PASSWORD_REQUIRED(isSuccess = true, isActionRequired = true),
    IMPORT_SUCCESS_AUTHORIZATION_REQUIRED(isSuccess = true, isActionRequired = true),
    IMPORT_FAILURE(isSuccess = false, isActionRequired = false),
}

enum class ButtonState {
    DISABLED,
    ENABLED,
    INVISIBLE,
    GONE,
}

enum class StatusText {
    HIDDEN,
    IMPORTING_PROGRESS,
    IMPORT_SUCCESS,
    IMPORT_SUCCESS_PASSWORD_REQUIRED,
    IMPORT_SUCCESS_AUTHORIZATION_REQUIRED,
    IMPORT_SUCCESS_PASSWORD_AND_AUTHORIZATION_REQUIRED,
    IMPORT_READ_FAILURE,
    IMPORT_PARTIAL_FAILURE,
    IMPORT_FAILURE,
}

enum class CloseButtonLabel {
    OK,
    LATER,
}
