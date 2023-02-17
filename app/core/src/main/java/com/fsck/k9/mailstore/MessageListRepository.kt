package com.fsck.k9.mailstore

import java.util.concurrent.CopyOnWriteArraySet

class MessageListRepository(
    private val messageStoreManager: MessageStoreManager,
) {
    private val globalListeners = CopyOnWriteArraySet<MessageListChangedListener>()
    private val accountListeners = CopyOnWriteArraySet<Pair<String, MessageListChangedListener>>()

    fun addListener(listener: MessageListChangedListener) {
        globalListeners.add(listener)
    }

    fun addListener(accountUuid: String, listener: MessageListChangedListener) {
        accountListeners.add(accountUuid to listener)
    }

    fun removeListener(listener: MessageListChangedListener) {
        globalListeners.remove(listener)

        val accountEntries = accountListeners.filter { it.second == listener }.toSet()
        if (accountEntries.isNotEmpty()) {
            accountListeners.removeAll(accountEntries)
        }
    }

    fun notifyMessageListChanged(accountUuid: String) {
        for (listener in globalListeners) {
            listener.onMessageListChanged()
        }

        for (listener in accountListeners) {
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
        messageMapper: MessageMapper<T>,
    ): List<T> {
        val messageStore = messageStoreManager.getMessageStore(accountUuid)
        val cache = MessageListCache.getCache(accountUuid)

        val mapper = if (cache.isEmpty()) messageMapper else CacheAwareMessageMapper(cache, messageMapper)
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
        messageMapper: MessageMapper<T>,
    ): List<T> {
        val messageStore = messageStoreManager.getMessageStore(accountUuid)
        val cache = MessageListCache.getCache(accountUuid)

        val mapper = if (cache.isEmpty()) messageMapper else CacheAwareMessageMapper(cache, messageMapper)
        return messageStore.getThreadedMessages(selection, selectionArgs, sortOrder, mapper)
    }

    /**
     * Retrieve list of messages in a thread from [MessageStore] but override values with data from [MessageListCache].
     */
    fun <T> getThread(
        accountUuid: String,
        threadId: Long,
        sortOrder: String,
        messageMapper: MessageMapper<T>,
    ): List<T> {
        val messageStore = messageStoreManager.getMessageStore(accountUuid)
        val cache = MessageListCache.getCache(accountUuid)

        val mapper = if (cache.isEmpty()) messageMapper else CacheAwareMessageMapper(cache, messageMapper)
        return messageStore.getThread(threadId, sortOrder, mapper)
    }
}

fun interface MessageListChangedListener {
    fun onMessageListChanged()
}
