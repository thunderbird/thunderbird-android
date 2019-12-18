package com.fsck.k9.ui.choosefolder

import androidx.lifecycle.ViewModel
import com.fsck.k9.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.ui.folders.FoldersLiveData
import com.fsck.k9.ui.folders.FoldersLiveDataFactory

class ChooseFolderViewModel(private val foldersLiveDataFactory: FoldersLiveDataFactory) : ViewModel() {
    private var foldersLiveData: FoldersLiveData? = null

    fun getFolders(account: Account, displayMode: FolderMode): FoldersLiveData {
        val liveData = foldersLiveData
        if (liveData != null && liveData.accountUuid == account.uuid && liveData.displayMode == displayMode) {
            return liveData
        }

        return foldersLiveDataFactory.create(account, displayMode).also {
            foldersLiveData = it
        }
    }
}
