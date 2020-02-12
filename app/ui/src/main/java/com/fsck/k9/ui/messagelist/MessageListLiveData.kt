package com.fsck.k9.ui.messagelist

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import androidx.lifecycle.LiveData
import com.fsck.k9.Preferences
import com.fsck.k9.provider.EmailProvider
import com.fsck.k9.search.getAccountUuids
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessageListLiveData(
    private val messageListLoader: MessageListLoader,
    private val preferences: Preferences,
    private val contentResolver: ContentResolver,
    private val coroutineScope: CoroutineScope,
    val config: MessageListConfig
) : LiveData<MessageListInfo>() {

    private val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            loadMessageListAsync()
        }
    }

    private fun loadMessageListAsync() {
        coroutineScope.launch(Dispatchers.Main) {
            value = withContext(Dispatchers.IO) {
                messageListLoader.getMessageList(config)
            }
        }
    }

    override fun onActive() {
        super.onActive()

        registerContentObserverAsync()
        loadMessageListAsync()
    }

    override fun onInactive() {
        super.onInactive()
        contentResolver.unregisterContentObserver(contentObserver)
    }

    private fun registerContentObserverAsync() {
        coroutineScope.launch(Dispatchers.Main) {
            val notificationUris = withContext(Dispatchers.IO) {
                getNotificationUris()
            }

            for (notificationUri in notificationUris) {
                contentResolver.registerContentObserver(notificationUri, false, contentObserver)
            }
        }
    }

    private fun getNotificationUris(): List<Uri> {
        val accountUuids = config.search.getAccountUuids(preferences)
        return accountUuids.map { accountUuid ->
            EmailProvider.getNotificationUri(accountUuid)
        }
    }
}
