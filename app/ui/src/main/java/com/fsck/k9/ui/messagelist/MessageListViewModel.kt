package com.fsck.k9.ui.messagelist

import androidx.lifecycle.ViewModel
import com.fsck.k9.Account
import com.fsck.k9.ui.folders.FoldersLiveData
import com.fsck.k9.ui.folders.FoldersLiveDataFactory

class MessageListViewModel(private val foldersLiveDataFactory: FoldersLiveDataFactory) : ViewModel() {
    private var foldersLiveData: FoldersLiveData? = null


    fun getFolders(account: Account): FoldersLiveData {
        val liveData = foldersLiveData
        if (liveData != null && liveData.accountUuid == account.uuid) {
            return liveData
        }

        return foldersLiveDataFactory.create(account).also {
            foldersLiveData = it
        }
    }
}
