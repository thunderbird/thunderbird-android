package com.fsck.k9.ui.folders

import androidx.lifecycle.LiveData
import com.fsck.k9.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.AccountsChangeListener
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.controller.SimpleMessagingListener
import com.fsck.k9.mailstore.DisplayFolder
import com.fsck.k9.mailstore.FolderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FoldersLiveData(
    private val folderRepository: FolderRepository,
    private val messagingController: MessagingController,
    private val preferences: Preferences,
    val accountUuid: String,
    val displayMode: FolderMode?
) : LiveData<List<DisplayFolder>>() {

    private val messagingListener = object : SimpleMessagingListener() {
        override fun folderStatusChanged(
            account: Account,
            folderId: Long
        ) {
            if (account?.uuid == accountUuid) {
                loadFoldersAsync()
            }
        }
    }

    private val accountsListener = AccountsChangeListener {
        loadFoldersAsync()
    }

    private fun loadFoldersAsync() {
        GlobalScope.launch(Dispatchers.Main) {
            value = withContext(Dispatchers.IO) { folderRepository.getDisplayFolders(displayMode) }
        }
    }

    override fun onActive() {
        super.onActive()
        messagingController.addListener(messagingListener)
        preferences.addOnAccountsChangeListener(accountsListener)
        loadFoldersAsync()
    }

    override fun onInactive() {
        super.onInactive()
        messagingController.removeListener(messagingListener)
        preferences.removeOnAccountsChangeListener(accountsListener)
    }
}
