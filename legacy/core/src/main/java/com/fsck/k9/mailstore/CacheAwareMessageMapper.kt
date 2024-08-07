package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.MessageDetailsAccessor
import app.k9mail.legacy.mailstore.MessageMapper
import com.fsck.k9.mail.Flag

internal class CacheAwareMessageMapper<T>(
    private val cache: MessageListCache,
    private val messageMapper: MessageMapper<T>,
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
    private val message: MessageDetailsAccessor,
) : MessageDetailsAccessor by message {
    override val isRead: Boolean
        get() {
            return cache.getFlagForMessage(message.id, Flag.SEEN)
                ?: cache.getFlagForThread(message.threadRoot, Flag.SEEN)
                ?: message.isRead
        }

    override val isStarred: Boolean
        get() {
            return cache.getFlagForMessage(message.id, Flag.FLAGGED)
                ?: cache.getFlagForThread(message.threadRoot, Flag.FLAGGED)
                ?: message.isStarred
        }

    override val isAnswered: Boolean
        get() {
            return cache.getFlagForMessage(message.id, Flag.ANSWERED)
                ?: cache.getFlagForThread(message.threadRoot, Flag.ANSWERED)
                ?: message.isAnswered
        }

    override val isForwarded: Boolean
        get() {
            return cache.getFlagForMessage(message.id, Flag.FORWARDED)
                ?: cache.getFlagForThread(message.threadRoot, Flag.FORWARDED)
                ?: message.isForwarded
        }
}
