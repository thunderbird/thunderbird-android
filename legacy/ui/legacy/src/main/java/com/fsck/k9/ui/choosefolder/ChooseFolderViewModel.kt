package com.fsck.k9.ui.choosefolder

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.ui.folder.DisplayFolder
import app.k9mail.legacy.ui.folder.DisplayFolderRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
class ChooseFolderViewModel(
    private val folderRepository: DisplayFolderRepository,
) : ViewModel() {
    private val inputFlow = MutableSharedFlow<DisplayMode>(replay = 1)
    private val foldersFlow = inputFlow
// TODO MBAL remove this commented code
//        .flatMapLatest { (account, showHiddenFolders) ->
//            folderRepository.getDisplayFoldersFlow(account, showHiddenFolders)
        .flatMapLatest { (accounts, showHiddenFolders) ->
            getCombinedFoldersFlow(accounts, showHiddenFolders)
        }

    var isShowHiddenFolders: Boolean = false
        private set

    // MBAL -- get the combined folders from all accounts
    fun getCombinedFoldersFlow(accounts: List<Account>, showHiddenFolders: Boolean): Flow<List<DisplayFolder>> {
        val folderFlows = accounts.map { account ->
            folderRepository.getDisplayFoldersFlow(account, showHiddenFolders)
        }
        return combine(folderFlows) { folderLists: Array<List<DisplayFolder>> ->
            folderLists.toList().flatten()
        }
    }
    fun getFolders(): LiveData<List<DisplayFolder>> {
        return foldersFlow.asLiveData()
    }

    // TODO MBAL remove this function
    /*
    fun setDisplayMode(account: Account, showHiddenFolders: Boolean) {
        isShowHiddenFolders = showHiddenFolders
        viewModelScope.launch {
            inputFlow.emit(DisplayMode(account, showHiddenFolders))
        }
    }
    */

    fun setDisplayMode(accounts: List<Account>, showHiddenFolders: Boolean) {
        isShowHiddenFolders = showHiddenFolders
        viewModelScope.launch {
            inputFlow.emit(DisplayMode(accounts, showHiddenFolders))
        }
    }}

// TODO MBAL remove this class
//private data class DisplayMode(val account: Account, val showHiddenFolders: Boolean)
private data class DisplayMode(val accounts: List<Account>, val showHiddenFolders: Boolean)
