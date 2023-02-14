package com.fsck.k9.ui.messagelist

import com.fsck.k9.Preferences
import com.fsck.k9.mailstore.MessageListRepository
import kotlinx.coroutines.CoroutineScope

class MessageListLiveDataFactory(
    private val messageListLoader: MessageListLoader,
    private val preferences: Preferences,
    private val messageListRepository: MessageListRepository,
) {
    fun create(coroutineScope: CoroutineScope, config: MessageListConfig): MessageListLiveData {
        return MessageListLiveData(messageListLoader, preferences, messageListRepository, coroutineScope, config)
    }
}
