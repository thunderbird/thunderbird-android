package com.fsck.k9.mailstore

import app.k9mail.legacy.di.DI
import com.fsck.k9.mail.Flag
import kotlin.collections.set

typealias MessageId = Long
typealias ThreadId = Long
typealias FolderId = Long
typealias FlagValue = Boolean
typealias AccountUuid = String

/**
 * Cache to bridge the time needed to write (user-initiated) changes to the database.
 */
class MessageListCache private constructor(private val accountUuid: String) {
    private val messageCache = mutableMapOf<MessageId, MutableMap<Flag, FlagValue>>()
    private val threadCache = mutableMapOf<ThreadId, MutableMap<Flag, FlagValue>>()
    private val hiddenMessageCache = mutableMapOf<MessageId, FolderId>()

    fun getFlagForMessage(messageId: Long, flag: Flag): Boolean? {
        synchronized(messageCache) {
            val columnMap = messageCache[messageId]
            return columnMap?.get(flag)
        }
    }

    fun getFlagForThread(threadRootId: Long, flag: Flag): Boolean? {
        synchronized(threadCache) {
            val columnMap = threadCache[threadRootId]
            return columnMap?.get(flag)
        }
    }

    fun setFlagForMessages(messageIds: List<Long>, flag: Flag, value: Boolean) {
        synchronized(messageCache) {
            for (messageId in messageIds) {
                val columnMap = messageCache.getOrPut(messageId) { mutableMapOf() }
                columnMap[flag] = value
            }
        }

        notifyChange()
    }

    fun setValueForThreads(threadRootIds: List<Long>, flag: Flag, value: Boolean) {
        synchronized(threadCache) {
            for (threadRootId in threadRootIds) {
                val columnMap = threadCache.getOrPut(threadRootId) { mutableMapOf() }
                columnMap[flag] = value
            }
        }

        notifyChange()
    }

    fun removeFlagForMessages(messageIds: List<Long>, flag: Flag) {
        synchronized(messageCache) {
            for (messageId in messageIds) {
                val columnMap = messageCache[messageId]
                if (columnMap != null) {
                    columnMap.remove(flag)
                    if (columnMap.isEmpty()) {
                        messageCache.remove(messageId)
                    }
                }
            }
        }
    }

    fun removeFlagForThreads(threadRootIds: List<Long>, flag: Flag) {
        synchronized(threadCache) {
            for (threadRootId in threadRootIds) {
                val columnMap = threadCache[threadRootId]
                if (columnMap != null) {
                    columnMap.remove(flag)
                    if (columnMap.isEmpty()) {
                        threadCache.remove(threadRootId)
                    }
                }
            }
        }
    }

    fun hideMessages(messages: List<LocalMessage>) {
        synchronized(hiddenMessageCache) {
            for (message in messages) {
                val messageId = message.databaseId
                val folderId = message.folder.databaseId
                hiddenMessageCache[messageId] = folderId
            }
        }

        notifyChange()
    }

    fun isMessageHidden(messageId: Long, folderId: Long): Boolean {
        synchronized(hiddenMessageCache) {
            val hiddenInFolder = hiddenMessageCache[messageId]
            return hiddenInFolder == folderId
        }
    }

    fun unhideMessages(messages: List<LocalMessage>) {
        synchronized(hiddenMessageCache) {
            for (message in messages) {
                val messageId = message.databaseId
                val folderId = message.folder.databaseId
                val hiddenInFolder = hiddenMessageCache[messageId]
                if (hiddenInFolder == folderId) {
                    hiddenMessageCache.remove(messageId)
                }
            }
        }
    }

    fun isEmpty(): Boolean {
        return isMessageCacheEmpty() && isThreadCacheEmpty() && isHiddenMessageCacheEmpty()
    }

    private fun isMessageCacheEmpty(): Boolean {
        return synchronized(messageCache) { messageCache.isEmpty() }
    }

    private fun isThreadCacheEmpty(): Boolean {
        return synchronized(threadCache) { threadCache.isEmpty() }
    }

    private fun isHiddenMessageCacheEmpty(): Boolean {
        return synchronized(hiddenMessageCache) { hiddenMessageCache.isEmpty() }
    }

    private fun notifyChange() {
        val messageListRepository = DI.get<MessageListRepository>()
        messageListRepository.notifyMessageListChanged(accountUuid)
    }

    companion object {
        private val instances = mutableMapOf<AccountUuid, MessageListCache>()

        @JvmStatic
        @Synchronized
        fun getCache(accountUuid: String): MessageListCache {
            return instances.getOrPut(accountUuid) { MessageListCache(accountUuid) }
        }
    }
}
