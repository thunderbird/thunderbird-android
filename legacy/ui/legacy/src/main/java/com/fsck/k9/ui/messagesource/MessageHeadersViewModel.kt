package com.fsck.k9.ui.messagesource

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.mail.Header
import com.fsck.k9.mailstore.MessageRepository
import com.fsck.k9.ui.base.loader.LoaderState
import com.fsck.k9.ui.base.loader.liveDataLoader

private typealias MessageHeaderState = LoaderState<List<Header>>

class MessageHeadersViewModel(private val messageRepository: MessageRepository) : ViewModel() {
    private var messageHeaderLiveData: LiveData<MessageHeaderState>? = null

    fun loadHeaders(messageReference: MessageReference): LiveData<MessageHeaderState> {
        return messageHeaderLiveData ?: loadMessageHeader(messageReference).also { messageHeaderLiveData = it }
    }

    private fun loadMessageHeader(messageReference: MessageReference): LiveData<MessageHeaderState> {
        return liveDataLoader {
            messageRepository.getHeaders(messageReference)
        }
    }
}
