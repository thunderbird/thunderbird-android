package com.fsck.k9.ui.messagelist

import androidx.lifecycle.LiveData
import app.k9mail.legacy.mailstore.MessageListChangedListener
import app.k9mail.legacy.mailstore.MessageListRepository
import com.fsck.k9.search.getLegacyAccountUuids
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccountManager

class MessageListLiveData(
    private val messageListLoader: MessageListLoader,
    private val accountManager: LegacyAccountManager,
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
            val accountUuids = config.search.getLegacyAccountUuids(accountManager)

            for (accountUuid in accountUuids) {
                messageListRepository.addListener(accountUuid, messageListChangedListener)
            }
        }
    }
}
