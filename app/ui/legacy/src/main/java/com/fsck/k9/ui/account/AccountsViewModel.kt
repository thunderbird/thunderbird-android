package com.fsck.k9.ui.account

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.fsck.k9.Account
import com.fsck.k9.controller.MessageCounts
import com.fsck.k9.controller.MessageCountsProvider
import com.fsck.k9.preferences.AccountManager
import com.fsck.k9.provider.EmailProvider
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
    private val contentResolver: ContentResolver
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
                        starredMessageCount = messageCounts.starred
                    )
                }
            }
        }

    private fun getMessageCountsFlow(account: Account): Flow<MessageCounts> {
        return callbackFlow {
            val notificationUri = EmailProvider.getNotificationUri(account.uuid)

            send(messageCountsProvider.getMessageCounts(account))

            val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    launch {
                        send(messageCountsProvider.getMessageCounts(account))
                    }
                }
            }
            contentResolver.registerContentObserver(notificationUri, false, contentObserver)

            awaitClose {
                contentResolver.unregisterContentObserver(contentObserver)
            }
        }.flowOn(Dispatchers.IO)
    }

    val displayAccountsLiveData: LiveData<List<DisplayAccount>> = displayAccountFlow.asLiveData()
}
