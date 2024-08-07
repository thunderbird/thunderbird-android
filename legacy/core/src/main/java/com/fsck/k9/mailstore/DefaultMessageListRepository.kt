package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.MessageListChangedListener
import app.k9mail.legacy.mailstore.MessageListRepository
import app.k9mail.legacy.mailstore.MessageMapper
import app.k9mail.legacy.mailstore.MessageStoreManager
import java.util.concurrent.CopyOnWriteArraySet

class DefaultMessageListRepository(
    private val messageStoreManager: MessageStoreManager,
) : MessageListRepository {
    private val globalListeners = CopyOnWriteArraySet<MessageListChangedListener>()
    private val accountListeners = CopyOnWriteArraySet<Pair<String, MessageListChangedListener>>()

    override fun addListener(listener: MessageListChangedListener) {
        globalListeners.add(listener)
    }

    override fun addListener(accountUuid: String, listener: MessageListChangedListener) {
        accountListeners.add(accountUuid to listener)
    }

    override fun removeListener(listener: MessageListChangedListener) {
        globalListeners.remove(listener)

        val accountEntries = accountListeners.filter { it.second == listener }.toSet()
        if (accountEntries.isNotEmpty()) {
            accountListeners.removeAll(accountEntries)
        }
    }

    override fun notifyMessageListChanged(accountUuid: String) {
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
    override fun <T> getMessages(
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
    override fun <T> getThreadedMessages(
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
    override fun <T> getThread(
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
