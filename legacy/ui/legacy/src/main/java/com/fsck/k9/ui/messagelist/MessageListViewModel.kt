package com.fsck.k9.ui.messagelist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.k9mail.legacy.message.controller.MessageReference
import java.util.LinkedList
import net.thunderbird.core.logging.Logger

class MessageListViewModel(
    private val messageListLiveDataFactory: MessageListLiveDataFactory,
    private val logger: Logger,
) : ViewModel() {
    private var currentMessageListLiveData: MessageListLiveData? = null
    private val messageListLiveData = MediatorLiveData<MessageListInfo>()

    val messageSortOverrides = LinkedList<Pair<MessageReference, MessageSortOverride>>()

    fun getMessageListLiveData(): LiveData<MessageListInfo> {
        return messageListLiveData
    }

    fun loadMessageList(config: MessageListConfig, forceUpdate: Boolean = false) {
        logger.debug { "loadMessageList() called with: config = $config, forceUpdate = $forceUpdate" }
        if (!forceUpdate && currentMessageListLiveData?.config == config) return

        removeCurrentMessageListLiveData()

        val liveData = messageListLiveDataFactory.create(viewModelScope, config)
        currentMessageListLiveData = liveData

        messageListLiveData.addSource(liveData) { items ->
            logger.debug { "Received new MessageListInfo: $items" }
            messageListLiveData.value = items
        }
    }

    private fun removeCurrentMessageListLiveData() {
        currentMessageListLiveData?.let {
            currentMessageListLiveData = null
            messageListLiveData.removeSource(it)
        }
    }
}
