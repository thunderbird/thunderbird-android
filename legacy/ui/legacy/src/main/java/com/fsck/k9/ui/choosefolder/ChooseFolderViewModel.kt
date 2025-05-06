package com.fsck.k9.ui.choosefolder

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import app.k9mail.legacy.account.LegacyAccount
import app.k9mail.legacy.ui.folder.DisplayFolder
import app.k9mail.legacy.ui.folder.DisplayFolderRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChooseFolderViewModel(
    private val folderRepository: DisplayFolderRepository,
) : ViewModel() {
    private val inputFlow = MutableSharedFlow<DisplayMode>(replay = 1)
    private val foldersFlow = inputFlow
        .flatMapLatest { (account, showHiddenFolders) ->
            folderRepository.getDisplayFoldersFlow(account, showHiddenFolders)
        }

    var isShowHiddenFolders: Boolean = false
        private set

    fun getFolders(): LiveData<List<DisplayFolder>> {
        return foldersFlow.asLiveData()
    }

    fun setDisplayMode(account: LegacyAccount, showHiddenFolders: Boolean) {
        isShowHiddenFolders = showHiddenFolders
        viewModelScope.launch {
            inputFlow.emit(DisplayMode(account, showHiddenFolders))
        }
    }
}

private data class DisplayMode(val account: LegacyAccount, val showHiddenFolders: Boolean)
