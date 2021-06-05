package com.fsck.k9.ui.account

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.fsck.k9.Account
import com.fsck.k9.AccountsChangeListener
import com.fsck.k9.Preferences
import com.fsck.k9.controller.UnreadMessageCountProvider
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
    val preferences: Preferences,
    val unreadMessageCountProvider: UnreadMessageCountProvider,
    private val contentResolver: ContentResolver
) :
    ViewModel() {

    private val accountsFlow: Flow<List<Account>> =
        callbackFlow {
            send(preferences.accounts)

            val accountsChangeListener = AccountsChangeListener {
                launch {
                    send(preferences.accounts)
                }
            }
            preferences.addOnAccountsChangeListener(accountsChangeListener)
            awaitClose {
                preferences.removeOnAccountsChangeListener(accountsChangeListener)
            }
        }.flowOn(Dispatchers.IO)

    private val displayAccountFlow: Flow<List<DisplayAccount>> = accountsFlow
        .flatMapLatest { accounts ->

            val unreadCountFlows: List<Flow<Int>> = accounts.map { account ->
                getUnreadCountFlow(account)
            }

            combine(unreadCountFlows) { unreadCounts ->
                unreadCounts.mapIndexed { index, unreadCount ->
                    DisplayAccount(account = accounts[index], unreadCount)
                }
            }
        }

    private fun getUnreadCountFlow(account: Account): Flow<Int> {
        return callbackFlow {

            val notificationUri = EmailProvider.getNotificationUri(account.uuid)

            send(unreadMessageCountProvider.getUnreadMessageCount(account))

            val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    launch {
                        send(unreadMessageCountProvider.getUnreadMessageCount(account))
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
