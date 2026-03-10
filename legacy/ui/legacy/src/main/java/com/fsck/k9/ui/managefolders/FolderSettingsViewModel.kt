package com.fsck.k9.ui.managefolders

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import app.k9mail.legacy.mailstore.FolderRepository
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.helper.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderDetails

private const val NO_FOLDER_ID = 0L

class FolderSettingsViewModel(
    private val preferences: Preferences,
    private val folderRepository: FolderRepository,
    private val messagingController: MessagingController,
) : ViewModel() {
    private val actionLiveData = SingleLiveEvent<Action>()
    private var folderSettingsLiveData: LiveData<FolderSettingsResult>? = null

    private lateinit var account: LegacyAccountDto
    private var folderId: Long = NO_FOLDER_ID

    val showClearFolderInMenu: Boolean
        get() = this::account.isInitialized && folderId != NO_FOLDER_ID

    fun getFolderSettingsLiveData(accountUuid: String, folderId: Long): LiveData<FolderSettingsResult> {
        return folderSettingsLiveData ?: createFolderSettingsLiveData(accountUuid, folderId).also {
            folderSettingsLiveData = it
        }
    }

    private fun createFolderSettingsLiveData(
        accountUuid: String,
        folderId: Long,
    ): LiveData<FolderSettingsResult> {
        return liveData(context = viewModelScope.coroutineContext) {
            val account = loadAccount(accountUuid)
            val folderDetails = folderRepository.loadFolderDetails(account, folderId)
            if (folderDetails == null) {
                Log.w("Folder with ID $folderId not found")
                emit(FolderNotFound)
                return@liveData
            }

            this@FolderSettingsViewModel.account = account
            this@FolderSettingsViewModel.folderId = folderId

            val folderSettingsData = FolderSettingsData(
                folder = folderDetails.folder,
                dataStore = FolderSettingsDataStore(folderRepository, account.id, folderDetails),
            )
            emit(folderSettingsData)
        }
    }

    private suspend fun loadAccount(accountUuid: String): LegacyAccountDto {
        return withContext(Dispatchers.IO) {
            preferences.getAccount(accountUuid) ?: error("Missing account: $accountUuid")
        }
    }

    private suspend fun FolderRepository.loadFolderDetails(account: LegacyAccountDto, folderId: Long): FolderDetails? {
        return withContext(Dispatchers.IO) {
            getFolderDetails(account.id, folderId)
        }
    }

    fun showClearFolderConfirmationDialog() {
        sendActionEvent(Action.ShowClearFolderConfirmationDialog)
    }

    fun onClearFolderConfirmation() {
        messagingController.clearFolder(account, folderId)
    }

    fun getActionEvents(): LiveData<Action> = actionLiveData

    private fun sendActionEvent(action: Action) {
        actionLiveData.value = action
    }
}

sealed class FolderSettingsResult
object FolderNotFound : FolderSettingsResult()
data class FolderSettingsData(val folder: Folder, val dataStore: FolderSettingsDataStore) : FolderSettingsResult()

sealed class Action {
    object ShowClearFolderConfirmationDialog : Action()
}
