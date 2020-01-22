package com.fsck.k9.ui.messagelist

import android.content.ContentResolver
import com.fsck.k9.Preferences
import kotlinx.coroutines.CoroutineScope

class MessageListLiveDataFactory(
    private val messageListLoader: MessageListLoader,
    private val preferences: Preferences,
    private val contentResolver: ContentResolver
) {
    fun create(coroutineScope: CoroutineScope, config: MessageListConfig): MessageListLiveData {
        return MessageListLiveData(messageListLoader, preferences, contentResolver, coroutineScope, config)
    }
}
