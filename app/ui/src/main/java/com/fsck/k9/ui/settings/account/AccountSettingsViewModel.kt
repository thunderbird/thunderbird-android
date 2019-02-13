package com.fsck.k9.ui.settings.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.mailstore.FolderRepositoryManager
import com.fsck.k9.mailstore.RemoteFolderInfo
import com.fsck.k9.ui.account.AccountsLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class AccountSettingsViewModel(
        private val preferences: Preferences,
        private val folderRepositoryManager: FolderRepositoryManager
) : ViewModel() {
    public val accounts = AccountsLiveData(preferences)
    private val accountLiveData = MutableLiveData<Account>()
    private val foldersLiveData = MutableLiveData<RemoteFolderInfo>()

    fun getAccount(accountUuid: String): LiveData<Account> {
        if (accountLiveData.value == null) {

            GlobalScope.launch(Dispatchers.Main) {
                val account = async {
                    loadAccount(accountUuid)
                }.await()

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
            val remoteFolderInfo = async {
                folderRepository.getRemoteFolderInfo()
            }.await()

            foldersLiveData.value = remoteFolderInfo
        }
    }
}
