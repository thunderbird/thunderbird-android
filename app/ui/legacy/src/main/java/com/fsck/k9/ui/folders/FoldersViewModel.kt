package com.fsck.k9.ui.folders

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.controller.MessageCountsProvider
import com.fsck.k9.mailstore.DisplayFolder
import com.fsck.k9.mailstore.FolderRepository
import com.fsck.k9.search.SearchAccount
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FoldersViewModel(
    private val folderRepository: FolderRepository,
    private val messageCountsProvider: MessageCountsProvider,
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val inputFlow = MutableSharedFlow<Account?>(replay = 1)
    private val foldersFlow = inputFlow
        .flatMapLatest { account ->
            if (account == null) {
                flowOf(emptyList())
            } else {
                folderRepository.getDisplayFoldersFlow(account)
            }
        }
        .map { displayFolders ->
            FolderList(unifiedInbox = createDisplayUnifiedInbox(), folders = displayFolders)
        }
        .flowOn(backgroundDispatcher)

    private fun createDisplayUnifiedInbox(): DisplayUnifiedInbox? {
        return getUnifiedInboxAccount()?.let { searchAccount ->
            val messageCounts = messageCountsProvider.getMessageCounts(searchAccount)
            DisplayUnifiedInbox(messageCounts.unread, messageCounts.starred)
        }
    }

    private fun getUnifiedInboxAccount(): SearchAccount? {
        return if (K9.isShowUnifiedInbox) SearchAccount.createUnifiedInboxAccount() else null
    }

    fun getFolderListLiveData(): LiveData<FolderList> {
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

data class FolderList(
    val unifiedInbox: DisplayUnifiedInbox?,
    val folders: List<DisplayFolder>
)

data class DisplayUnifiedInbox(
    val unreadMessageCount: Int,
    val starredMessageCount: Int
)
