package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.MessageListChangedListener
import app.k9mail.legacy.mailstore.MessageListRepository
import app.k9mail.legacy.mailstore.MessageMapper
import app.k9mail.legacy.mailstore.MessageStoreManager
import java.util.concurrent.CopyOnWriteArraySet
import net.thunderbird.feature.account.AccountId

class DefaultMessageListRepository(
    private val messageStoreManager: MessageStoreManager,
) : MessageListRepository {
    private val globalListeners = CopyOnWriteArraySet<MessageListChangedListener>()
    private val accountListeners = CopyOnWriteArraySet<Pair<AccountId, MessageListChangedListener>>()

    override fun addListener(listener: MessageListChangedListener) {
        globalListeners.add(listener)
    }

    override fun addListener(accountId: AccountId, listener: MessageListChangedListener) {
        accountListeners.add(accountId to listener)
    }

    override fun removeListener(listener: MessageListChangedListener) {
        globalListeners.remove(listener)

        val accountEntries = accountListeners.filter { it.second == listener }.toSet()
        if (accountEntries.isNotEmpty()) {
            accountListeners.removeAll(accountEntries)
        }
    }

    override fun notifyMessageListChanged(accountId: AccountId) {
        for (listener in globalListeners) {
            listener.onMessageListChanged()
        }

        for (listener in accountListeners) {
            if (listener.first == accountId) {
                listener.second.onMessageListChanged()
            }
        }
    }

    /**
     * Retrieve list of messages from [MessageStore] but override values with data from [MessageListCache].
     */
    override fun <T> getMessages(
        accountId: AccountId,
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        messageMapper: MessageMapper<T>,
    ): List<T> {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        val cache = MessageListCache.getCache(accountId)

        val mapper = if (cache.isEmpty()) messageMapper else CacheAwareMessageMapper(cache, messageMapper)
        return messageStore.getMessages(selection, selectionArgs, sortOrder, mapper)
    }

    /**
     * Retrieve threaded list of messages from [MessageStore] but override values with data from [MessageListCache].
     */
    override fun <T> getThreadedMessages(
        accountId: AccountId,
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        messageMapper: MessageMapper<T>,
    ): List<T> {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        val cache = MessageListCache.getCache(accountId)

        val mapper = if (cache.isEmpty()) messageMapper else CacheAwareMessageMapper(cache, messageMapper)
        return messageStore.getThreadedMessages(selection, selectionArgs, sortOrder, mapper)
    }

    /**
     * Retrieve list of messages in a thread from [MessageStore] but override values with data from [MessageListCache].
     */
    override fun <T> getThread(
        accountId: AccountId,
        threadId: Long,
        sortOrder: String,
        messageMapper: MessageMapper<T>,
    ): List<T> {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        val cache = MessageListCache.getCache(accountId)

        val mapper = if (cache.isEmpty()) messageMapper else CacheAwareMessageMapper(cache, messageMapper)
        return messageStore.getThread(threadId, sortOrder, mapper)
    }
}
