package com.fsck.k9.ui.settings.encryption

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fsck.k9.Preferences
import com.fsck.k9.ui.helper.CoroutineScopeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private typealias AccountUuid = String
private typealias AccountNumber = Int

class SettingsEncryptionViewModel(val context: Context, val preferences: Preferences) : CoroutineScopeViewModel() {
    private val uiModelLiveData = MutableLiveData<SettingsEncryptionUiModel>()

    private val uiModel = SettingsEncryptionUiModel()
    private var accountsMap: Map<AccountNumber, AccountUuid> = emptyMap()
    private var savedSelection: SavedListItemSelection? = null
    private var contentUri: Uri? = null

    fun getUiModel(): LiveData<SettingsEncryptionUiModel> {
        if (uiModelLiveData.value == null) {
            uiModelLiveData.value = uiModel

            launch {
                val accounts = withContext(Dispatchers.IO) { preferences.accounts }

                accountsMap = accounts.map { it.accountNumber to it.uuid }.toMap()

                val listItems = savedSelection.let { savedState ->
                    var accountListItems = emptyList<SettingsListItem>()
                    for (account in accounts) {
                        accountListItems = account.identities.mapIndexed { index, identity ->
                            // TODO: better IDs
                            SettingsListItem.EncryptionIdentity(account.accountNumber * 1000 + index, account.accountNumber, identity.email).apply {
                                enabled = savedState == null || account.uuid in savedState.selectedAccountUuids
                            }
                        }
                    }

                    val advancedSettings = SettingsListItem.AdvancedSettings.apply {
                        enabled = savedState == null || savedState.includeGeneralSettings
                    }

                    accountListItems + listOf(advancedSettings)
                }

                updateUiModel {
                    initializeSettingsList(listItems)
                }
            }
        }

        return uiModelLiveData
    }


//    fun initializeFromSavedState(savedInstanceState: Bundle) {
//        savedSelection = SavedListItemSelection(
//                includeGeneralSettings = savedInstanceState.getBoolean(STATE_INCLUDE_GENERAL_SETTINGS),
//                selectedAccountUuids = savedInstanceState.getStringArray(STATE_SELECTED_ACCOUNTS)?.toSet() ?: emptySet()
//        )
//
//        uiModel.apply {
//            isSettingsListEnabled = savedInstanceState.getBoolean(STATE_SETTINGS_LIST_ENABLED)
////            exportButton = ButtonState.valueOf(
////                    savedInstanceState.getString(STATE_EXPORT_BUTTON, ButtonState.DISABLED.name)
////            )
////            isShareButtonVisible = savedInstanceState.getBoolean(STATE_SHARE_BUTTON_VISIBLE)
////            isProgressVisible = savedInstanceState.getBoolean(STATE_PROGRESS_VISIBLE)
////            statusText = StatusText.valueOf(savedInstanceState.getString(STATE_STATUS_TEXT, StatusText.HIDDEN.name))
//        }
//
//        contentUri = savedInstanceState.getParcelable(STATE_CONTENT_URI)
//    }

    fun saveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_SETTINGS_LIST_ENABLED, uiModel.isSettingsListEnabled)

        outState.putParcelable(STATE_CONTENT_URI, contentUri)
    }

    fun onSettingsListItemSelected(position: Int, isSelected: Boolean) {
        savedSelection = null

        updateUiModel {
            setSettingsListItemSelection(position, isSelected)
        }
    }

    private fun updateUiModel(block: SettingsEncryptionUiModel.() -> Unit) {
        uiModel.block()
        uiModelLiveData.value = uiModel
    }

    companion object {
        private const val MIN_PROGRESS_DURATION = 1000L
        private const val SETTINGS_MIME_TYPE = "application/octet-stream"

        private const val STATE_SETTINGS_LIST_ENABLED = "settingsListEnabled"
        private const val STATE_CONTENT_URI = "contentUri"
    }
}

private data class SavedListItemSelection(
        val includeGeneralSettings: Boolean,
        val selectedAccountUuids: Set<AccountUuid>
)
