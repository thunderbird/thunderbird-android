package com.fsck.k9.ui.folders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.fsck.k9.Account
import com.fsck.k9.mailstore.DisplayFolder

class FoldersViewModel(private val foldersLiveDataFactory: FoldersLiveDataFactory) : ViewModel() {
    private var currentFoldersLiveData: FoldersLiveData? = null
    private val foldersLiveData = MediatorLiveData<List<DisplayFolder>>()

    fun getFolderListLiveData(): LiveData<List<DisplayFolder>> {
        return foldersLiveData
    }

    fun loadFolders(account: Account) {
        if (currentFoldersLiveData?.accountUuid == account.uuid) return

        removeCurrentFoldersLiveData()

        val liveData = foldersLiveDataFactory.create(account)
        currentFoldersLiveData = liveData

        foldersLiveData.addSource(liveData) { items ->
            foldersLiveData.value = items
        }
    }

    fun stopLoadingFolders() {
        removeCurrentFoldersLiveData()
        foldersLiveData.value = null
    }

    private fun removeCurrentFoldersLiveData() {
        currentFoldersLiveData?.let {
            currentFoldersLiveData = null
            foldersLiveData.removeSource(it)
        }
    }
}
