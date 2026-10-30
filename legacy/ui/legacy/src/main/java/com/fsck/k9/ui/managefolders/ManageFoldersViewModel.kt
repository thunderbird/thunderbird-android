package com.fsck.k9.ui.managefolders

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import app.k9mail.legacy.ui.folder.DisplayFolder
import app.k9mail.legacy.ui.folder.DisplayFolderRepository
import net.thunderbird.feature.account.AccountId

class ManageFoldersViewModel(
    private val folderRepository: DisplayFolderRepository,
) : ViewModel() {
    fun getFolders(accountId: AccountId): LiveData<List<DisplayFolder>> {
        return folderRepository.getDisplayFoldersFlow(accountId, includeHiddenFolders = true).asLiveData()
    }
}
