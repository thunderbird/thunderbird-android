package com.fsck.k9.ui.messagelist

import androidx.lifecycle.LiveData
import com.fsck.k9.mailstore.MessageListChangedListener
import com.fsck.k9.mailstore.MessageListRepository
import com.fsck.k9.preferences.AccountManager
import com.fsck.k9.search.getAccountUuids
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessageListLiveData(
    private val messageListLoader: MessageListLoader,
    private val accountManager: AccountManager,
    private val messageListRepository: MessageListRepository,
    private val coroutineScope: CoroutineScope,
    val config: MessageListConfig,
) : LiveData<MessageListInfo>() {

    private val messageListChangedListener = MessageListChangedListener {
        loadMessageListAsync()
    }

    private fun loadMessageListAsync() {
        coroutineScope.launch(Dispatchers.Main) {
            val messageList = withContext(Dispatchers.IO) {
                messageListLoader.getMessageList(config)
            }
            value = messageList
        }
    }

    override fun onActive() {
        super.onActive()

        registerMessageListChangedListenerAsync()
        loadMessageListAsync()
    }

    override fun onInactive() {
        super.onInactive()
        messageListRepository.removeListener(messageListChangedListener)
    }

    private fun registerMessageListChangedListenerAsync() {
        coroutineScope.launch(Dispatchers.IO) {
            val accountUuids = config.search.getAccountUuids(accountManager)

            for (accountUuid in accountUuids) {
                messageListRepository.addListener(accountUuid, messageListChangedListener)
            }
        }
    }
}
