package com.fsck.k9.ui.settings.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.fsck.k9.Account
import com.fsck.k9.mailstore.FolderRepository
import com.fsck.k9.mailstore.FolderType
import com.fsck.k9.mailstore.RemoteFolder
import com.fsck.k9.mailstore.SpecialFolderSelectionStrategy
import com.fsck.k9.preferences.AccountManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountSettingsViewModel(
    private val accountManager: AccountManager,
    private val folderRepository: FolderRepository,
    private val specialFolderSelectionStrategy: SpecialFolderSelectionStrategy,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    val accounts = accountManager.getAccountsFlow().asLiveData()
    private var accountUuid: String? = null
    private val accountLiveData = MutableLiveData<Account?>()
    private val foldersLiveData = MutableLiveData<RemoteFolderInfo>()

    fun getAccount(accountUuid: String): LiveData<Account?> {
        if (this.accountUuid != accountUuid) {
            this.accountUuid = accountUuid
            viewModelScope.launch {
                val account = withContext(backgroundDispatcher) {
                    loadAccount(accountUuid)
                }
                accountLiveData.value = account
            }
        }

        return accountLiveData
    }

    /**
     * Returns the cached [Account] if possible. Otherwise does a blocking load because `PreferenceFragmentCompat`
     * doesn't support asynchronous preference loading.
     */
    fun getAccountBlocking(accountUuid: String): Account {
        return accountLiveData.value
            ?: loadAccount(accountUuid).also { account ->
                this.accountUuid = accountUuid
                accountLiveData.value = account
            }
            ?: error("Account $accountUuid not found")
    }

    private fun loadAccount(accountUuid: String): Account? {
        return accountManager.getAccount(accountUuid)
    }

    fun getFolders(account: Account): LiveData<RemoteFolderInfo> {
        if (foldersLiveData.value == null) {
            loadFolders(account)
        }

        return foldersLiveData
    }

    private fun loadFolders(account: Account) {
        viewModelScope.launch {
            val remoteFolderInfo = withContext(backgroundDispatcher) {
                val folders = folderRepository.getRemoteFolders(account)
                val automaticSpecialFolders = getAutomaticSpecialFolders(folders)
                RemoteFolderInfo(folders, automaticSpecialFolders)
            }
            foldersLiveData.value = remoteFolderInfo
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
