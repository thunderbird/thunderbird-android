package com.fsck.k9.ui.managefolders

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import app.k9mail.legacy.ui.folder.DisplayFolder
import app.k9mail.legacy.ui.folder.DisplayFolderRepository
import net.thunderbird.core.android.account.LegacyAccountDto

class ManageFoldersViewModel(
    private val folderRepository: DisplayFolderRepository,
) : ViewModel() {
    fun getFolders(account: LegacyAccountDto): LiveData<List<DisplayFolder>> {
        return folderRepository.getDisplayFoldersFlow(account, includeHiddenFolders = true).asLiveData()
    }
}
