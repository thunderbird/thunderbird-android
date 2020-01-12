package com.fsck.k9.ui.messagelist

import android.content.ContentResolver
import kotlinx.coroutines.CoroutineScope

class MessageListLiveDataFactory(
    private val messageListLoader: MessageListLoader,
    private val contentResolver: ContentResolver
) {
    fun create(coroutineScope: CoroutineScope, config: MessageListConfig): MessageListLiveData {
        return MessageListLiveData(messageListLoader, contentResolver, coroutineScope, config)
    }
}
