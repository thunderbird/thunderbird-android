package com.fsck.k9.mailstore

internal class CacheAwareMessageMapper<T>(
    private val cache: MessageListCache,
    private val messageMapper: MessageMapper<T>
) : MessageMapper<T?> {
    override fun map(message: MessageDetailsAccessor): T? {
        val messageId = message.id
        val folderId = message.folderId

        if (cache.isMessageHidden(messageId, folderId)) {
            return null
        }

        val cachedMessage = CacheAwareMessageDetailsAccessor(cache, message)
        return messageMapper.map(cachedMessage)
    }
}

private class CacheAwareMessageDetailsAccessor(
    private val cache: MessageListCache,
    private val message: MessageDetailsAccessor
) : MessageDetailsAccessor by message {
    override val isRead: Boolean
        get() {
            return cache.getValueForMessage(message.id, "read")?.let { it == "1" }
                ?: cache.getValueForThread(message.threadRoot, "read")?.let { it == "1" }
                ?: message.isRead
        }

    override val isStarred: Boolean
        get() {
            return cache.getValueForMessage(message.id, "flagged")?.let { it == "1" }
                ?: cache.getValueForThread(message.threadRoot, "flagged")?.let { it == "1" }
                ?: message.isStarred
        }

    override val isAnswered: Boolean
        get() {
            return cache.getValueForMessage(message.id, "answered")?.let { it == "1" }
                ?: cache.getValueForThread(message.threadRoot, "answered")?.let { it == "1" }
                ?: message.isAnswered
        }

    override val isForwarded: Boolean
        get() {
            return cache.getValueForMessage(message.id, "forwarded")?.let { it == "1" }
                ?: cache.getValueForThread(message.threadRoot, "forwarded")?.let { it == "1" }
                ?: message.isForwarded
        }
}
