package app.k9mail.feature.navigation.drawer.legacy

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.message.controller.MessageCountsProvider
import app.k9mail.legacy.search.SearchAccount
import app.k9mail.legacy.ui.folder.DisplayFolderRepository
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
    private val folderRepository: DisplayFolderRepository,
    private val messageCountsProvider: MessageCountsProvider,
    private val isShowUnifiedInbox: () -> Boolean,
    private val getUnifiedInboxTitle: () -> String,
    private val getUnifiedInboxDetail: () -> String,
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val inputFlow = MutableSharedFlow<Account?>(replay = 1)
    private val foldersFlow = inputFlow
        .flatMapLatest { account ->
            if (account == null) {
                flowOf(0 to emptyList())
            } else {
                folderRepository.getDisplayFoldersFlow(account.uuid)
                    .map { displayFolders ->
                        account.accountNumber to displayFolders
                    }
            }
        }
        .map { (accountNumber, displayFolders) ->
            FolderList(
                unifiedInbox = createDisplayUnifiedInbox(),
                accountId = accountNumber + 1,
                folders = displayFolders,
            )
        }
        .flowOn(backgroundDispatcher)

    private fun createDisplayUnifiedInbox(): DisplayUnifiedInbox? {
        return getUnifiedInboxAccount()?.let { searchAccount ->
            val messageCounts = messageCountsProvider.getMessageCounts(searchAccount)
            DisplayUnifiedInbox(messageCounts.unread, messageCounts.starred)
        }
    }

    private fun getUnifiedInboxAccount(): SearchAccount? {
        return if (isShowUnifiedInbox()) {
            SearchAccount.createUnifiedInboxAccount(
                unifiedInboxTitle = getUnifiedInboxTitle(),
                unifiedInboxDetail = getUnifiedInboxDetail(),
            )
        } else {
            null
        }
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
