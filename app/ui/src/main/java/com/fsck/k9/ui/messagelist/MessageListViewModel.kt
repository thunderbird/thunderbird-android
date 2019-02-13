package com.fsck.k9.ui.messagelist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fsck.k9.Account
import com.fsck.k9.mailstore.Folder
import com.fsck.k9.mailstore.FolderRepositoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MessageListViewModel(private val folderRepositoryManager: FolderRepositoryManager) : ViewModel() {
    private val foldersLiveData = MutableLiveData<List<Folder>>()
    private var account: Account? = null


    fun getFolders(account: Account): LiveData<List<Folder>> {
        if (foldersLiveData.value == null || this.account != account) {
            this.account = account
            loadFolders(account)
        }

        return foldersLiveData
    }

    private fun loadFolders(account: Account) {
        GlobalScope.launch(Dispatchers.Main) {
            val folders = async {
                val folderRepository = folderRepositoryManager.getFolderRepository(account)
                folderRepository.getDisplayFolders()
            }.await()

            foldersLiveData.value = folders
        }
    }
}
