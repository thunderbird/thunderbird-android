package com.fsck.k9.ui.folders

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.fsck.k9.Account
import com.fsck.k9.mailstore.DisplayFolder
import com.fsck.k9.mailstore.FolderRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FoldersViewModel(private val folderRepository: FolderRepository) : ViewModel() {
    private val inputFlow = MutableSharedFlow<Account?>(replay = 1)
    private val foldersFlow = inputFlow
        .flatMapLatest { account ->
            if (account == null) {
                flowOf(emptyList())
            } else {
                folderRepository.getDisplayFoldersFlow(account)
            }
        }

    fun getFolderListLiveData(): LiveData<List<DisplayFolder>> {
        return foldersFlow.asLiveData()
    }

    fun loadFolders(account: Account) {
        viewModelScope.launch {
            // When switching accounts we want to remove the old list right away, not keep it until the new list
            // has been loaded.
            inputFlow.emit(null)

            inputFlow.emit(account)
        }
    }
}
