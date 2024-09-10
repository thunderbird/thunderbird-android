package app.k9mail.feature.navigation.drawer.legacy

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.account.AccountManager
import app.k9mail.legacy.mailstore.MessageListChangedListener
import app.k9mail.legacy.mailstore.MessageListRepository
import app.k9mail.legacy.message.controller.MessageCounts
import app.k9mail.legacy.message.controller.MessageCountsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModel(
    accountManager: AccountManager,
    private val messageCountsProvider: MessageCountsProvider,
    private val messageListRepository: MessageListRepository,
) : ViewModel() {
    private val displayAccountFlow: Flow<List<DisplayAccount>> = accountManager.getAccountsFlow()
        .flatMapLatest { accounts ->
            val messageCountsFlows: List<Flow<MessageCounts>> = accounts.map { account ->
                getMessageCountsFlow(account)
            }

            combine(messageCountsFlows) { messageCountsList ->
                messageCountsList.mapIndexed { index, messageCounts ->
                    DisplayAccount(
                        account = accounts[index],
                        unreadMessageCount = messageCounts.unread,
                        starredMessageCount = messageCounts.starred,
                    )
                }
            }
        }

    private fun getMessageCountsFlow(account: Account): Flow<MessageCounts> {
        return callbackFlow {
            send(messageCountsProvider.getMessageCounts(account))

            val listener = MessageListChangedListener {
                launch {
                    send(messageCountsProvider.getMessageCounts(account))
                }
            }
            messageListRepository.addListener(account.uuid, listener)

            awaitClose {
                messageListRepository.removeListener(listener)
            }
        }.flowOn(Dispatchers.IO)
    }

    val displayAccountsLiveData: LiveData<List<DisplayAccount>> = displayAccountFlow.asLiveData()
}
