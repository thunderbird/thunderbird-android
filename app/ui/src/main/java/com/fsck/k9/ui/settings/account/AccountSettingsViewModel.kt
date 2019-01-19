package com.fsck.k9.ui.settings.account

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.mailstore.FolderRepositoryManager
import com.fsck.k9.mailstore.RemoteFolderInfo
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg

class AccountSettingsViewModel(
        private val preferences: Preferences,
        private val folderRepositoryManager: FolderRepositoryManager
) : ViewModel() {
    private val accountLiveData = MutableLiveData<Account>()
    private val foldersLiveData = MutableLiveData<RemoteFolderInfo>()

    fun getAccount(accountUuid: String): LiveData<Account> {
        if (accountLiveData.value == null) {
            launch(UI) {
                val account = bg {
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
        launch(UI) {
            val remoteFolderInfo = bg {
                folderRepository.getRemoteFolderInfo()
            }.await()

            foldersLiveData.value = remoteFolderInfo
        }
    }
}
