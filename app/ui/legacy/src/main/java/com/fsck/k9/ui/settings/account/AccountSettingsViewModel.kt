package com.fsck.k9.ui.settings.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.mailstore.FolderRepositoryManager
import com.fsck.k9.mailstore.FolderType
import com.fsck.k9.mailstore.RemoteFolder
import com.fsck.k9.mailstore.SpecialFolderSelectionStrategy
import com.fsck.k9.ui.account.AccountsLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountSettingsViewModel(
    private val preferences: Preferences,
    private val folderRepositoryManager: FolderRepositoryManager,
    private val specialFolderSelectionStrategy: SpecialFolderSelectionStrategy
) : ViewModel() {
    val accounts = AccountsLiveData(preferences)
    private val accountLiveData = MutableLiveData<Account>()
    private val foldersLiveData = MutableLiveData<RemoteFolderInfo>()

    fun getAccount(accountUuid: String): LiveData<Account> {
        if (accountLiveData.value == null) {

            GlobalScope.launch(Dispatchers.Main) {
                accountLiveData.value = withContext(Dispatchers.IO) {
                    loadAccount(accountUuid)
                }
            }
        }

        return accountLiveData
    }

    /**
     * Returns the cached [Account] if possible. Otherwise does a blocking load because `PreferenceFragmentCompat`
     * doesn't support asynchronous preference loading.
     */
    fun getAccountBlocking(accountUuid: String): Account {
        return accountLiveData.value ?: loadAccount(accountUuid).also {
            accountLiveData.value = it
        }
    }

    private fun loadAccount(accountUuid: String) = preferences.getAccount(accountUuid)

    fun getFolders(account: Account): LiveData<RemoteFolderInfo> {
        if (foldersLiveData.value == null) {
            loadFolders(account)
        }

        return foldersLiveData
    }

    private fun loadFolders(account: Account) {
        val folderRepository = folderRepositoryManager.getFolderRepository(account)
        GlobalScope.launch(Dispatchers.Main) {
            foldersLiveData.value = withContext(Dispatchers.IO) {
                val folders = folderRepository.getRemoteFolders()
                val automaticSpecialFolders = getAutomaticSpecialFolders(folders)
                RemoteFolderInfo(folders, automaticSpecialFolders)
            }
        }
    }

    private fun getAutomaticSpecialFolders(folders: List<RemoteFolder>): Map<FolderType, RemoteFolder?> {
        return mapOf(
            FolderType.ARCHIVE to specialFolderSelectionStrategy.selectSpecialFolder(folders, FolderType.ARCHIVE),
            FolderType.DRAFTS to specialFolderSelectionStrategy.selectSpecialFolder(folders, FolderType.DRAFTS),
            FolderType.SENT to specialFolderSelectionStrategy.selectSpecialFolder(folders, FolderType.SENT),
            FolderType.SPAM to specialFolderSelectionStrategy.selectSpecialFolder(folders, FolderType.SPAM),
            FolderType.TRASH to specialFolderSelectionStrategy.selectSpecialFolder(folders, FolderType.TRASH)
        )
    }
}

data class RemoteFolderInfo(
    val folders: List<RemoteFolder>,
    val automaticSpecialFolders: Map<FolderType, RemoteFolder?>
)
