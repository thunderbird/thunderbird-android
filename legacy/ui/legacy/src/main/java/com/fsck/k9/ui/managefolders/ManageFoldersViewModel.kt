package com.fsck.k9.ui.managefolders

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.folder.DisplayFolder
import com.fsck.k9.mailstore.FolderRepository

class ManageFoldersViewModel(private val folderRepository: FolderRepository) : ViewModel() {
    fun getFolders(account: Account): LiveData<List<DisplayFolder>> {
        return folderRepository.getDisplayFoldersFlow(account).asLiveData()
    }
}
