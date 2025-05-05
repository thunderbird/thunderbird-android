package com.fsck.k9.ui.messagelist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.logging.Timber
import java.util.LinkedList

class MessageListViewModel(private val messageListLiveDataFactory: MessageListLiveDataFactory) : ViewModel() {
    private var currentMessageListLiveData: MessageListLiveData? = null
    private val messageListLiveData = MediatorLiveData<MessageListInfo>()

    val messageSortOverrides = LinkedList<Pair<MessageReference, MessageSortOverride>>()

    fun getMessageListLiveData(): LiveData<MessageListInfo> {
        return messageListLiveData
    }

    fun loadMessageList(config: MessageListConfig, forceUpdate: Boolean = false) {
        Timber.d("loadMessageList() called with: config = $config, forceUpdate = $forceUpdate")
        if (!forceUpdate && currentMessageListLiveData?.config == config) return

        removeCurrentMessageListLiveData()

        val liveData = messageListLiveDataFactory.create(viewModelScope, config)
        currentMessageListLiveData = liveData

        messageListLiveData.addSource(liveData) { items ->
            Timber.d("Received new MessageListInfo: $items")
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
