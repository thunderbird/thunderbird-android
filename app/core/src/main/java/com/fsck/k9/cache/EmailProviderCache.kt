package com.fsck.k9.cache

import com.fsck.k9.DI
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.MessageListRepository
import kotlin.collections.set

typealias MessageId = Long
typealias ThreadId = Long
typealias FolderId = Long
typealias ColumnName = String
typealias ColumnValue = String
typealias AccountUuid = String

/**
 * Cache to bridge the time needed to write (user-initiated) changes to the database.
 */
class EmailProviderCache private constructor(private val accountUuid: String) {
    private val messageCache = mutableMapOf<MessageId, MutableMap<ColumnName, ColumnValue>>()
    private val threadCache = mutableMapOf<ThreadId, MutableMap<ColumnName, ColumnValue>>()
    private val hiddenMessageCache = mutableMapOf<MessageId, FolderId>()

    fun getValueForMessage(messageId: Long, columnName: String): String? {
        synchronized(messageCache) {
            val columnMap = messageCache[messageId]
            return columnMap?.get(columnName)
        }
    }

    fun getValueForThread(threadRootId: Long, columnName: String): String? {
        synchronized(threadCache) {
            val columnMap = threadCache[threadRootId]
            return columnMap?.get(columnName)
        }
    }

    fun setValueForMessages(messageIds: List<Long>, columnName: String, value: String) {
        synchronized(messageCache) {
            for (messageId in messageIds) {
                val columnMap = messageCache.getOrPut(messageId) { mutableMapOf() }
                columnMap[columnName] = value
            }
        }

        notifyChange()
    }

    fun setValueForThreads(threadRootIds: List<Long>, columnName: String, value: String) {
        synchronized(threadCache) {
            for (threadRootId in threadRootIds) {
                val columnMap = threadCache.getOrPut(threadRootId) { mutableMapOf() }
                columnMap[columnName] = value
            }
        }

        notifyChange()
    }

    fun removeValueForMessages(messageIds: List<Long>, columnName: String) {
        synchronized(messageCache) {
            for (messageId in messageIds) {
                val columnMap = messageCache[messageId]
                if (columnMap != null) {
                    columnMap.remove(columnName)
                    if (columnMap.isEmpty()) {
                        messageCache.remove(messageId)
                    }
                }
            }
        }
    }

    fun removeValueForThreads(threadRootIds: List<Long>, columnName: String) {
        synchronized(threadCache) {
            for (threadRootId in threadRootIds) {
                val columnMap = threadCache[threadRootId]
                if (columnMap != null) {
                    columnMap.remove(columnName)
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

    private fun notifyChange() {
        val messageListRepository = DI.get<MessageListRepository>()
        messageListRepository.notifyMessageListChanged(accountUuid)
    }

    companion object {
        private val instances = mutableMapOf<AccountUuid, EmailProviderCache>()

        @JvmStatic
        @Synchronized
        fun getCache(accountUuid: String): EmailProviderCache {
            return instances.getOrPut(accountUuid) { EmailProviderCache(accountUuid) }
        }
    }
}
