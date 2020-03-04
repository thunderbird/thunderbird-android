package com.fsck.k9.ui.managefolders

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.activity.FolderInfoHolder
import com.fsck.k9.mailstore.Folder
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalStoreProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FolderSettingsViewModel(
    private val preferences: Preferences,
    private val localStoreProvider: LocalStoreProvider
) : ViewModel() {
    private var folderSettingsLiveData: LiveData<FolderSettingsData>? = null

    fun getFolderSettingsLiveData(accountUuid: String, folderServerId: String): LiveData<FolderSettingsData> {
        return folderSettingsLiveData ?: createFolderSettingsLiveData(accountUuid, folderServerId).also {
            folderSettingsLiveData = it
        }
    }

    private fun createFolderSettingsLiveData(
        accountUuid: String,
        folderServerId: String
    ): LiveData<FolderSettingsData> {
        return liveData(context = viewModelScope.coroutineContext) {
            val account = loadAccount(accountUuid)
            val localFolder = loadLocalFolder(account, folderServerId)

            val folderSettingsData = FolderSettingsData(
                folder = createFolderObject(account, folderServerId, localFolder),
                dataStore = FolderSettingsDataStore(localFolder)
            )
            emit(folderSettingsData)
        }
    }

    private suspend fun loadAccount(accountUuid: String): Account {
        return withContext(Dispatchers.IO) {
            preferences.getAccount(accountUuid) ?: error("Missing account: $accountUuid")
        }
    }

    private suspend fun loadLocalFolder(account: Account, folderServerId: String): LocalFolder {
        return withContext(Dispatchers.IO) {
            val localStore = localStoreProvider.getInstance(account)
            val folder = localStore.getFolder(folderServerId)
            folder.open()
            folder
        }
    }

    private fun createFolderObject(account: Account, folderServerId: String, localFolder: LocalFolder): Folder {
        val folderType = FolderInfoHolder.getFolderType(account, folderServerId)
        return Folder(id = -1, serverId = folderServerId, name = localFolder.name, type = folderType)
    }
}

data class FolderSettingsData(val folder: Folder, val dataStore: FolderSettingsDataStore)
