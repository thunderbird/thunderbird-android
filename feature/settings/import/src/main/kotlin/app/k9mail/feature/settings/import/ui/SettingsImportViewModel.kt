package app.k9mail.feature.settings.import.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.k9mail.feature.settings.import.SettingsImportExternalContract.AccountActivator
import com.fsck.k9.helper.SingleLiveEvent
import com.fsck.k9.helper.measureRealtimeMillisWithResult
import com.fsck.k9.preferences.ImportContents
import com.fsck.k9.preferences.ImportResults
import com.fsck.k9.preferences.SettingsImporter
import com.fsck.k9.ui.base.bundle.getEnum
import com.fsck.k9.ui.base.bundle.putEnum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import timber.log.Timber

private typealias AccountUuid = String
private typealias AccountNumber = Int

internal class SettingsImportViewModel(
    private val context: Context,
    private val settingsImporter: SettingsImporter,
    private val accountActivator: AccountActivator,
) : ViewModel() {
    private val uiModelLiveData = MutableLiveData<SettingsImportUiModel>()
    private val actionLiveData = SingleLiveEvent<Action>()

    private val uiModel = SettingsImportUiModel()
    private var accountsMap: MutableMap<AccountNumber, AccountUuid> = mutableMapOf()
    private val accountStateMap: MutableMap<AccountNumber, AccountState> = mutableMapOf()
    private var contentUri: Uri? = null
    private var currentlyAuthorizingAccountUuid: String? = null

    private val containsGeneralSettings: Boolean
        get() = uiModel.settingsList.any { it is SettingsListItem.GeneralSettings }

    private val includeGeneralSettings: Boolean
        get() = uiModel.settingsList.first { it is SettingsListItem.GeneralSettings }.selected

    private val generalSettingsImportStatus
        get() = uiModel.settingsList.first { it is SettingsListItem.GeneralSettings }.importStatus

    private val selectedAccounts: Set<AccountUuid>
        get() {
            return uiModel.settingsList.asSequence()
                .filterIsInstance<SettingsListItem.Account>()
                .filter { it.selected }
                .map {
                    accountsMap[it.accountIndex] ?: error("Unknown account index: ${it.accountIndex}")
                }
                .toSet()
        }

    fun getActionEvents(): LiveData<Action> = actionLiveData

    fun getUiModel(): LiveData<SettingsImportUiModel> {
        if (uiModelLiveData.value == null) {
            uiModelLiveData.value = uiModel
        }

        return uiModelLiveData
    }

    fun initializeFromSavedState(savedInstanceState: Bundle) {
        contentUri = BundleCompat.getParcelable(savedInstanceState, STATE_CONTENT_URI, Uri::class.java)
        currentlyAuthorizingAccountUuid = savedInstanceState.getString(STATE_CURRENTLY_AUTHORIZING_ACCOUNT_UUID)

        updateUiModel {
            isSettingsListVisible = savedInstanceState.getBoolean(STATE_SETTINGS_LIST_VISIBLE)
            isSettingsListEnabled = savedInstanceState.getBoolean(STATE_SETTINGS_LIST_ENABLED)
            importButton = savedInstanceState.getEnum(STATE_IMPORT_BUTTON, ButtonState.DISABLED)
            closeButton = savedInstanceState.getEnum(STATE_CLOSE_BUTTON, ButtonState.DISABLED)
            closeButtonLabel = savedInstanceState.getEnum(STATE_CLOSE_BUTTON_LABEL, CloseButtonLabel.OK)
            isPickDocumentButtonVisible = savedInstanceState.getBoolean(STATE_PICK_DOCUMENT_BUTTON_VISIBLE)
            isPickDocumentButtonEnabled = savedInstanceState.getBoolean(STATE_PICK_DOCUMENT_BUTTON_ENABLED)

            isLoadingProgressVisible = savedInstanceState.getBoolean(STATE_LOADING_PROGRESS_VISIBLE)
            isImportProgressVisible = savedInstanceState.getBoolean(STATE_IMPORT_PROGRESS_VISIBLE)
            statusText = savedInstanceState.getEnum(STATE_STATUS_TEXT, StatusText.HIDDEN)

            if (!hasDocumentBeenRead) return@updateUiModel

            val generalSettingsItem = if (savedInstanceState.getBoolean(STATE_CONTAINS_GENERAL_SETTINGS)) {
                val includeGeneralSettings = savedInstanceState.getBoolean(STATE_INCLUDE_GENERAL_SETTINGS)
                val generalSettingsImportState = savedInstanceState.getEnum(
                    STATE_GENERAL_SETTINGS_IMPORT_STATUS,
                    ImportStatus.NOT_AVAILABLE,
                )

                SettingsListItem.GeneralSettings().apply {
                    selected = includeGeneralSettings
                    importStatus = generalSettingsImportState
                    enabled = !hasImportStarted
                }
            } else {
                null
            }

            val savedAccountList = savedInstanceState.getParcelableArrayList<SavedAccountState>(STATE_ACCOUNT_LIST)!!

            savedAccountList.forEach { saved ->
                accountsMap[saved.accountIndex] = saved.accountUuid
                accountStateMap[saved.accountIndex] = AccountState(
                    saved.incomingServerName,
                    saved.outgoingServerName,
                    saved.incomingServerPasswordNeeded,
                    saved.outgoingServerPasswordNeeded,
                )
            }

            val accountSettingsItems = savedAccountList.map { saved ->
                SettingsListItem.Account(saved.accountIndex, saved.displayName).apply {
                    selected = saved.selected
                    importStatus = saved.importStatus
                    enabled = if (hasImportStarted) {
                        saved.importStatus == ImportStatus.IMPORT_SUCCESS_PASSWORD_REQUIRED
                    } else {
                        true
                    }
                }
            }

            settingsList = if (generalSettingsItem != null) {
                listOf(generalSettingsItem) + accountSettingsItems
            } else {
                accountSettingsItems
            }
        }
    }

    fun saveInstanceState(outState: Bundle) {
        with(uiModel) {
            outState.putBoolean(STATE_SETTINGS_LIST_VISIBLE, isSettingsListVisible)
            outState.putBoolean(STATE_SETTINGS_LIST_ENABLED, isSettingsListEnabled)
            outState.putEnum(STATE_IMPORT_BUTTON, importButton)
            outState.putEnum(STATE_CLOSE_BUTTON, closeButton)
            outState.putEnum(STATE_CLOSE_BUTTON_LABEL, closeButtonLabel)
            outState.putBoolean(STATE_PICK_DOCUMENT_BUTTON_VISIBLE, isPickDocumentButtonVisible)
            outState.putBoolean(STATE_PICK_DOCUMENT_BUTTON_ENABLED, isPickDocumentButtonEnabled)
            outState.putBoolean(STATE_LOADING_PROGRESS_VISIBLE, isLoadingProgressVisible)
            outState.putBoolean(STATE_IMPORT_PROGRESS_VISIBLE, isImportProgressVisible)
            outState.putEnum(STATE_STATUS_TEXT, statusText)
            outState.putString(STATE_CURRENTLY_AUTHORIZING_ACCOUNT_UUID, currentlyAuthorizingAccountUuid)

            if (hasDocumentBeenRead) {
                val containsGeneralSettings = this@SettingsImportViewModel.containsGeneralSettings
                outState.putBoolean(STATE_CONTAINS_GENERAL_SETTINGS, containsGeneralSettings)
                if (containsGeneralSettings) {
                    outState.putBoolean(STATE_INCLUDE_GENERAL_SETTINGS, includeGeneralSettings)
                    outState.putEnum(STATE_GENERAL_SETTINGS_IMPORT_STATUS, generalSettingsImportStatus)
                }
                outState.putParcelableArrayList(STATE_ACCOUNT_LIST, createSavedAccountList())
            }
        }

        outState.putParcelable(STATE_CONTENT_URI, contentUri)
    }

    fun onPickDocumentButtonClicked() {
        updateUiModel {
            disablePickDocumentButton()
        }

        sendActionEvent(Action.PickDocument)
    }

    fun onDocumentPickCanceled() {
        updateUiModel {
            enablePickDocumentButton()
        }
    }

    fun onDocumentPicked(contentUri: Uri) {
        updateUiModel {
            showLoadingProgress()
        }

        startReadSettingsFile(contentUri)
    }

    fun onImportButtonClicked() {
        updateUiModel {
            showImportingProgress()
        }

        startImportSettings()
    }

    fun onCloseButtonClicked() {
        sendActionEvent(Action.Close(importSuccess = uiModel.wasAccountImportSuccessful))
    }

    fun onSettingsListItemClicked(position: Int) {
        val settingsListItem = uiModel.settingsList[position]
        when (settingsListItem.importStatus) {
            ImportStatus.NOT_AVAILABLE -> updateUiModel {
                toggleSettingsListItemSelection(position)
            }

            ImportStatus.IMPORT_SUCCESS_AUTHORIZATION_REQUIRED -> {
                startAuthorization(settingsListItem as SettingsListItem.Account)
            }

            ImportStatus.IMPORT_SUCCESS_PASSWORD_REQUIRED -> {
                showPasswordPromptDialog(settingsListItem as SettingsListItem.Account)
            }

            else -> Unit
        }
    }

    fun onPasswordPromptResult(result: PasswordPromptResult) {
        updateUiModel {
            val index = getListIndexOfAccount(result.accountUuid)
            setSettingsListState(index, ImportStatus.IMPORT_SUCCESS)

            updateCloseButtonAndImportStatusText()
        }

        viewModelScope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                with(result) {
                    accountActivator.enableAccount(accountUuid, incomingServerPassword, outgoingServerPassword)
                }
            }
        }
    }

    fun onReturnAfterAuthorization() {
        currentlyAuthorizingAccountUuid?.let { accountUuid ->
            currentlyAuthorizingAccountUuid = null
            updateUiModel {
                val index = getListIndexOfAccount(accountUuid)
                setSettingsListState(index, ImportStatus.IMPORT_SUCCESS)
                updateCloseButtonAndImportStatusText()
            }

            viewModelScope.launch(Dispatchers.IO) {
                withContext(NonCancellable) {
                    accountActivator.enableAccount(accountUuid)
                }
            }
        }
    }

    private fun getListIndexOfAccount(accountUuid: String): Int {
        return uiModel.settingsList.indexOfFirst {
            it is SettingsListItem.Account && accountsMap[it.accountIndex] == accountUuid
        }
    }

    private fun startReadSettingsFile(contentUri: Uri) {
        this.contentUri = contentUri

        viewModelScope.launch {
            try {
                val (elapsed, contents) = measureRealtimeMillisWithResult {
                    withContext(Dispatchers.IO) {
                        readSettings(contentUri)
                    }
                }

                if (elapsed < MIN_PROGRESS_DURATION) {
                    delay(MIN_PROGRESS_DURATION - elapsed)
                }

                accountsMap = contents.accounts
                    .asSequence()
                    .mapIndexed { index, account -> index to account.uuid }
                    .toMap(mutableMapOf())

                val items = mutableListOf<SettingsListItem>()
                if (contents.globalSettings) {
                    items.add(SettingsListItem.GeneralSettings())
                }
                contents.accounts.mapIndexedTo(items) { index, accountDescription ->
                    SettingsListItem.Account(index, accountDescription.name)
                }

                updateUiModel {
                    initializeSettingsList(items)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error reading settings file")

                updateUiModel {
                    showReadFailureText()
                }
            }
        }
    }

    private fun startImportSettings() {
        val contentUri = this.contentUri ?: error("contentUri is missing")
        viewModelScope.launch {
            try {
                val containsGeneralSettings = this@SettingsImportViewModel.containsGeneralSettings
                val importGeneralSettings = containsGeneralSettings && includeGeneralSettings
                val importAccounts = selectedAccounts.toList()

                val (elapsed, importResults) = measureRealtimeMillisWithResult {
                    withContext(Dispatchers.IO) {
                        importSettings(contentUri, importGeneralSettings, importAccounts)
                    }
                }

                if (elapsed < MIN_PROGRESS_DURATION) {
                    delay(MIN_PROGRESS_DURATION - elapsed)
                }

                updateUiModel {
                    if (containsGeneralSettings) {
                        setGeneralSettingsImportStatus(importResults, importGeneralSettings)
                    }
                    setAccountsImportStatus(importResults)

                    updateCloseButtonAndImportStatusText()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error importing settings")

                updateUiModel {
                    showImportErrorText()
                }
            }
        }
    }

    private fun SettingsImportUiModel.setGeneralSettingsImportStatus(
        importResults: ImportResults,
        importGeneralSettings: Boolean,
    ) {
        val importStatus = when {
            importResults.globalSettings -> ImportStatus.IMPORT_SUCCESS
            importGeneralSettings -> ImportStatus.IMPORT_FAILURE
            else -> ImportStatus.NOT_SELECTED
        }
        setSettingsListState(0, importStatus)
    }

    private fun SettingsImportUiModel.setAccountsImportStatus(importResults: ImportResults) {
        val failedToImport = importResults.erroneousAccounts.map { it.uuid }.toHashSet()
        settingsList.forEachIndexed { index, listItem ->
            if (listItem !is SettingsListItem.Account) return@forEachIndexed

            val accountIndex = listItem.accountIndex
            val accountUuid = accountsMap[accountIndex]

            if (accountUuid in failedToImport) {
                setSettingsListState(index, ImportStatus.IMPORT_FAILURE)
            } else {
                val accountPair = importResults.importedAccounts.firstOrNull { it.original.uuid == accountUuid }
                if (accountPair != null) {
                    accountsMap[accountIndex] = accountPair.imported.uuid
                    listItem.displayName = accountPair.imported.name

                    if (accountPair.authorizationNeeded) {
                        setSettingsListState(index, ImportStatus.IMPORT_SUCCESS_AUTHORIZATION_REQUIRED)
                    } else if (accountPair.incomingPasswordNeeded || accountPair.outgoingPasswordNeeded) {
                        accountStateMap[accountIndex] = AccountState(
                            accountPair.incomingServerName,
                            accountPair.outgoingServerName,
                            accountPair.incomingPasswordNeeded,
                            accountPair.outgoingPasswordNeeded,
                        )

                        setSettingsListState(index, ImportStatus.IMPORT_SUCCESS_PASSWORD_REQUIRED)
                    } else {
                        setSettingsListState(index, ImportStatus.IMPORT_SUCCESS)
                    }
                } else {
                    setSettingsListState(index, ImportStatus.NOT_SELECTED)
                }
            }
        }
    }

    private fun readSettings(contentUri: Uri): ImportContents {
        val inputStream = context.contentResolver.openInputStream(contentUri)
            ?: error("Failed to open settings file for reading: $contentUri")

        return inputStream.use {
            settingsImporter.getImportStreamContents(inputStream)
        }
    }

    private fun importSettings(contentUri: Uri, generalSettings: Boolean, accounts: List<AccountUuid>): ImportResults {
        val inputStream = context.contentResolver.openInputStream(contentUri)
            ?: error("Failed to open settings file for reading: $contentUri")

        return inputStream.use {
            settingsImporter.importSettings(inputStream, generalSettings, accounts)
        }
    }

    private fun startAuthorization(settingsListItem: SettingsListItem.Account) {
        val accountIndex = settingsListItem.accountIndex
        val accountUuid = accountsMap[accountIndex]!!
        currentlyAuthorizingAccountUuid = accountUuid

        sendActionEvent(Action.StartAuthorization(accountUuid))
    }

    private fun showPasswordPromptDialog(settingsListItem: SettingsListItem.Account) {
        val accountIndex = settingsListItem.accountIndex

        val accountUuid = accountsMap[accountIndex]!!
        val accountState = accountStateMap[accountIndex]!!

        sendActionEvent(
            Action.PasswordPrompt(
                accountUuid,
                settingsListItem.displayName,
                accountState.incomingServerPasswordNeeded,
                accountState.incomingServerName,
                accountState.outgoingServerPasswordNeeded,
                accountState.outgoingServerName,
            ),
        )
    }

    private fun updateUiModel(block: SettingsImportUiModel.() -> Unit) {
        uiModel.block()
        uiModelLiveData.value = uiModel
    }

    private fun sendActionEvent(action: Action) {
        actionLiveData.value = action
    }

    private fun createSavedAccountList(): ArrayList<SavedAccountState> {
        return uiModel.settingsList
            .asSequence()
            .filterIsInstance<SettingsListItem.Account>()
            .map { accountItem ->
                val accountIndex = accountItem.accountIndex
                val accountState = accountStateMap[accountIndex]
                SavedAccountState(
                    accountIndex = accountIndex,
                    displayName = accountItem.displayName,
                    accountUuid = accountsMap[accountIndex]!!,
                    selected = accountItem.selected,
                    importStatus = accountItem.importStatus,
                    incomingServerName = accountState?.incomingServerName,
                    outgoingServerName = accountState?.outgoingServerName,
                    incomingServerPasswordNeeded = accountState?.incomingServerPasswordNeeded ?: false,
                    outgoingServerPasswordNeeded = accountState?.outgoingServerPasswordNeeded ?: false,
                )
            }
            .toCollection(ArrayList(uiModel.settingsList.size - 1))
    }

    companion object {
        private const val MIN_PROGRESS_DURATION = 500L

        private const val STATE_SETTINGS_LIST_VISIBLE = "settingsListVisible"
        private const val STATE_SETTINGS_LIST_ENABLED = "settingsListEnabled"
        private const val STATE_IMPORT_BUTTON = "importButton"
        private const val STATE_CLOSE_BUTTON = "closeButton"
        private const val STATE_CLOSE_BUTTON_LABEL = "closeButtonLabel"
        private const val STATE_PICK_DOCUMENT_BUTTON_VISIBLE = "pickDocumentButtonVisible"
        private const val STATE_PICK_DOCUMENT_BUTTON_ENABLED = "pickDocumentButtonEnabled"
        private const val STATE_LOADING_PROGRESS_VISIBLE = "loadingProgressVisible"
        private const val STATE_IMPORT_PROGRESS_VISIBLE = "importProgressVisible"
        private const val STATE_STATUS_TEXT = "statusText"
        private const val STATE_CONTAINS_GENERAL_SETTINGS = "containsGeneralSettings"
        private const val STATE_INCLUDE_GENERAL_SETTINGS = "includeGeneralSettings"
        private const val STATE_CONTENT_URI = "contentUri"
        private const val STATE_GENERAL_SETTINGS_IMPORT_STATUS = "generalSettingsImportStatus"
        private const val STATE_ACCOUNT_LIST = "accountList"
        private const val STATE_CURRENTLY_AUTHORIZING_ACCOUNT_UUID = "currentlyAuthorizingAccountUuid"
    }
}

sealed class Action {
    class Close(val importSuccess: Boolean) : Action()
    object PickDocument : Action()
    class StartAuthorization(val accountUuid: String) : Action()
    class PasswordPrompt(
        val accountUuid: String,
        val accountName: String,
        val inputIncomingServerPassword: Boolean,
        val incomingServerName: String?,
        val inputOutgoingServerPassword: Boolean,
        val outgoingServerName: String?,
    ) : Action()
}

private class AccountState(
    val incomingServerName: String?,
    val outgoingServerName: String?,
    val incomingServerPasswordNeeded: Boolean,
    val outgoingServerPasswordNeeded: Boolean,
)

@Suppress("LongParameterList")
@Parcelize
private class SavedAccountState(
    val accountIndex: Int,
    val displayName: String,
    val accountUuid: String,
    val selected: Boolean,
    val importStatus: ImportStatus,
    val incomingServerName: String?,
    val outgoingServerName: String?,
    val incomingServerPasswordNeeded: Boolean,
    val outgoingServerPasswordNeeded: Boolean,
) : Parcelable
