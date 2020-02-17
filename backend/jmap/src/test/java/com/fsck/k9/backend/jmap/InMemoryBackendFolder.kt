package com.fsck.k9.backend.jmap

import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.internet.MimeMessage
import java.util.Date
import okio.Buffer
import org.junit.Assert.assertEquals

class InMemoryBackendFolder(override var name: String, var type: FolderType) : BackendFolder {
    val extraStrings: MutableMap<String, String> = mutableMapOf()
    val extraNumbers: MutableMap<String, Long> = mutableMapOf()
    private val messages = mutableMapOf<String, Message>()
    private val messageFlags = mutableMapOf<String, MutableSet<Flag>>()

    override var visibleLimit: Int = 25

    fun assertMessages(vararg messagePairs: Pair<String, String>) {
        for ((messageServerId, resourceName) in messagePairs) {
            assertMessageContents(messageServerId, resourceName)
        }
        val messageServerIds = messagePairs.map { it.first }.toSet()
        assertEquals(messageServerIds, messages.keys)
    }

    private fun assertMessageContents(messageServerId: String, resourceName: String) {
        val message = messages[messageServerId] ?: error("Message $messageServerId not found")
        assertEquals(loadResource(resourceName), getMessageContents(message))
    }

    fun createMessages(vararg messagePairs: Pair<String, String>) {
        for ((messageServerId, resourceName) in messagePairs) {
            val inputStream = javaClass.getResourceAsStream(resourceName)
                ?: error("Couldn't load resource: $resourceName")

            val message = inputStream.use {
                MimeMessage.parseMimeMessage(inputStream, false)
            }

            messages[messageServerId] = message
            messageFlags[messageServerId] = mutableSetOf()
        }
    }

    private fun getMessageContents(message: Message): String {
        val buffer = Buffer()
        buffer.outputStream().use {
            message.writeTo(it)
        }
        return buffer.readUtf8()
    }

    override fun getMessageServerIds(): Set<String> {
        return messages.keys.toSet()
    }

    override fun getAllMessagesAndEffectiveDates(): Map<String, Long?> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun destroyMessages(messageServerIds: List<String>) {
        for (messageServerId in messageServerIds) {
            messages.remove(messageServerId)
            messageFlags.remove(messageServerId)
        }
    }

    override fun getLastUid(): Long? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getMoreMessages(): BackendFolder.MoreMessages {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setMoreMessages(moreMessages: BackendFolder.MoreMessages) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setLastChecked(timestamp: Long) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setStatus(status: String?) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getPushState(): String? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setPushState(pushState: String?) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun isMessagePresent(messageServerId: String): Boolean {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getMessageFlags(messageServerId: String): Set<Flag> {
        return messageFlags[messageServerId] ?: error("Message $messageServerId not found")
    }

    override fun setMessageFlag(messageServerId: String, flag: Flag, value: Boolean) {
        val flags = messageFlags[messageServerId] ?: error("Message $messageServerId not found")
        if (value) {
            flags.add(flag)
        } else {
            flags.remove(flag)
        }
    }

    override fun savePartialMessage(message: Message) {
        val messageServerId = checkNotNull(message.uid)
        messages[messageServerId] = message
        messageFlags[messageServerId] = message.flags.toMutableSet()
    }

    override fun saveCompleteMessage(message: Message) {
        val messageServerId = checkNotNull(message.uid)
        messages[messageServerId] = message
        messageFlags[messageServerId] = message.flags.toMutableSet()
    }

    override fun getLatestOldMessageSeenTime(): Date {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setLatestOldMessageSeenTime(date: Date) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getOldestMessageDate(): Date? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getFolderExtraString(name: String): String? = extraStrings[name]

    override fun setFolderExtraString(name: String, value: String?) {
        if (value != null) {
            extraStrings[name] = value
        } else {
            extraStrings.remove(name)
        }
    }

    override fun getFolderExtraNumber(name: String): Long? = extraNumbers[name]

    override fun setFolderExtraNumber(name: String, value: Long) {
        extraNumbers[name] = value
    }
}
