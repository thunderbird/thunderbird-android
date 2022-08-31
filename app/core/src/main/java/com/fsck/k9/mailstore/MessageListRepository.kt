package com.fsck.k9.mailstore

import java.util.concurrent.CopyOnWriteArraySet

class MessageListRepository(
    private val messageStoreManager: MessageStoreManager
) {
    private val listeners = CopyOnWriteArraySet<Pair<String, MessageListChangedListener>>()

    fun addListener(accountUuid: String, listener: MessageListChangedListener) {
        listeners.add(accountUuid to listener)
    }

    fun removeListener(listener: MessageListChangedListener) {
        val entries = listeners.filter { it.second == listener }.toSet()
        listeners.removeAll(entries)
    }

    fun notifyMessageListChanged(accountUuid: String) {
        for (listener in listeners) {
            if (listener.first == accountUuid) {
                listener.second.onMessageListChanged()
            }
        }
    }

    /**
     * Retrieve list of messages from [MessageStore] but override values with data from [MessageListCache].
     */
    fun <T> getMessages(
        accountUuid: String,
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        messageMapper: MessageMapper<T>
    ): List<T> {
        val messageStore = messageStoreManager.getMessageStore(accountUuid)
        val cache = MessageListCache.getCache(accountUuid)

        val mapper = CacheAwareMessageMapper(cache, messageMapper)
        return messageStore.getMessages(selection, selectionArgs, sortOrder, mapper)
    }

    /**
     * Retrieve threaded list of messages from [MessageStore] but override values with data from [MessageListCache].
     */
    fun <T> getThreadedMessages(
        accountUuid: String,
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        messageMapper: MessageMapper<T>
    ): List<T> {
        val messageStore = messageStoreManager.getMessageStore(accountUuid)
        val cache = MessageListCache.getCache(accountUuid)

        val mapper = CacheAwareMessageMapper(cache, messageMapper)
        return messageStore.getThreadedMessages(selection, selectionArgs, sortOrder, mapper)
    }

    /**
     * Retrieve list of messages in a thread from [MessageStore] but override values with data from [MessageListCache].
     */
    fun <T> getThread(
        accountUuid: String,
        threadId: Long,
        sortOrder: String,
        messageMapper: MessageMapper<T>
    ): List<T> {
        val messageStore = messageStoreManager.getMessageStore(accountUuid)
        val cache = MessageListCache.getCache(accountUuid)

        val mapper = CacheAwareMessageMapper(cache, messageMapper)
        return messageStore.getThread(threadId, sortOrder, mapper)
    }
}

fun interface MessageListChangedListener {
    fun onMessageListChanged()
}
