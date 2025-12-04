package app.k9mail.backend.testing

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendFolder.MoreMessages
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.internet.MimeMessage
import java.util.Date
import net.thunderbird.core.common.mail.Flag
import okio.Buffer
import okio.buffer
import okio.source

class InMemoryBackendFolder(override var name: String, var type: FolderType) : BackendFolder {
    val extraStrings: MutableMap<String, String> = mutableMapOf()
    val extraNumbers: MutableMap<String, Long> = mutableMapOf()
    private val messages = mutableMapOf<String, Message>()
    private val messageFlags = mutableMapOf<String, MutableSet<Flag>>()
    private var moreMessages: MoreMessages = MoreMessages.UNKNOWN
    private var status: String? = null
    private var lastChecked = 0L

    override var visibleLimit: Int = 25

    fun assertMessages(vararg messagePairs: Pair<String, String>) {
        for ((messageServerId, resourceName) in messagePairs) {
            assertMessageContents(messageServerId, resourceName)
        }
        val messageServerIds = messagePairs.map { it.first }.toSet()
        assertThat(messages.keys).isEqualTo(messageServerIds)
    }

    private fun assertMessageContents(messageServerId: String, resourceName: String) {
        val message = messages[messageServerId] ?: error("Message $messageServerId not found")
        assertThat(getMessageContents(message)).isEqualTo(loadResource(resourceName))
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
        return messages
            .map { (serverId, message) ->
                serverId to message.sentDate.time
            }
            .toMap()
    }

    override fun destroyMessages(messageServerIds: List<String>) {
        for (messageServerId in messageServerIds) {
            messages.remove(messageServerId)
            messageFlags.remove(messageServerId)
        }
    }

    override fun clearAllMessages() {
        destroyMessages(messages.keys.toList())
    }

    override fun getMoreMessages(): MoreMessages = moreMessages

    override fun setMoreMessages(moreMessages: MoreMessages) {
        this.moreMessages = moreMessages
    }

    override fun setLastChecked(timestamp: Long) {
        lastChecked = timestamp
    }

    override fun setStatus(status: String?) {
        this.status = status
    }

    override fun isMessagePresent(messageServerId: String): Boolean {
        return messages[messageServerId] != null
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

    override fun saveMessage(message: Message, downloadState: MessageDownloadState) {
        val messageServerId = checkNotNull(message.uid)
        messages[messageServerId] = message
        val flags = message.flags.toMutableSet()

        when (downloadState) {
            MessageDownloadState.ENVELOPE -> Unit
            MessageDownloadState.PARTIAL -> flags.add(Flag.X_DOWNLOADED_PARTIAL)
            MessageDownloadState.FULL -> flags.add(Flag.X_DOWNLOADED_FULL)
        }

        messageFlags[messageServerId] = flags
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

    private fun loadResource(name: String): String {
        val resourceAsStream = javaClass.getResourceAsStream(name) ?: error("Couldn't load resource: $name")
        return resourceAsStream.use { it.source().buffer().readUtf8() }
    }
}
