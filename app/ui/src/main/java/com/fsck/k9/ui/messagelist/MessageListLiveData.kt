package com.fsck.k9.ui.messagelist

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import androidx.lifecycle.LiveData
import com.fsck.k9.provider.EmailProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessageListLiveData(
    private val messageListLoader: MessageListLoader,
    private val contentResolver: ContentResolver,
    private val coroutineScope: CoroutineScope,
    val config: MessageListConfig
) : LiveData<List<MessageListItem>>() {
    private val notificationUris = config.search.accountUuids.map { accountUuid ->
        EmailProvider.getNotificationUri(accountUuid)
    }

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

        for (notificationUri in notificationUris) {
            contentResolver.registerContentObserver(notificationUri, false, contentObserver)
        }

        loadMessageListAsync()
    }

    override fun onInactive() {
        super.onInactive()

        for (notificationUri in notificationUris) {
            contentResolver.unregisterContentObserver(contentObserver)
        }
    }
}
