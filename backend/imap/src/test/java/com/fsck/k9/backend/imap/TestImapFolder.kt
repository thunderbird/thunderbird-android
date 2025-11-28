package com.fsck.k9.backend.imap

import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageRetrievalListener
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.store.imap.FetchListener
import com.fsck.k9.mail.store.imap.ImapFolder
import com.fsck.k9.mail.store.imap.ImapMessage
import com.fsck.k9.mail.store.imap.OpenMode
import com.fsck.k9.mail.store.imap.createImapMessage
import java.util.Date
import net.thunderbird.core.common.mail.Flag

open class TestImapFolder(override val serverId: String) : ImapFolder {
    override var mode: OpenMode? = null
        protected set

    override val isOpen: Boolean
        get() = mode != null

    override var messageCount: Int = 0

    var wasExpunged: Boolean = false
        private set

    val isClosed: Boolean
        get() = mode == null

    private val messages = mutableMapOf<Long, Message>()
    private val messageFlags = mutableMapOf<Long, MutableSet<Flag>>()
    private var uidValidity: Long? = null

    fun addMessage(uid: Long, message: Message) {
        require(!messages.containsKey(uid)) {
            "Folder '$serverId' already contains a message with the UID $uid"
        }

        messages[uid] = message
        messageFlags[uid] = mutableSetOf()

        messageCount = messages.size
    }

    fun removeAllMessages() {
        messages.clear()
        messageFlags.clear()
    }

    fun setUidValidity(value: Long) {
        uidValidity = value
    }

    override fun open(mode: OpenMode) {
        this.mode = mode
    }

    override fun close() {
        mode = null
    }

    override fun exists(): Boolean {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getUidValidity() = uidValidity

    override fun getMessage(uid: String): ImapMessage {
        return createImapMessage(uid)
    }

    override fun getUidFromMessageId(messageId: String): String? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getMessages(
        start: Int,
        end: Int,
        earliestDate: Date?,
        listener: MessageRetrievalListener<ImapMessage>?,
    ): List<ImapMessage> {
        require(start > 0)
        require(end >= start)
        require(end <= messages.size)

        return messages.keys.sortedDescending()
            .slice((start - 1) until end)
            .map { createImapMessage(uid = it.toString()) }
    }

    override fun areMoreMessagesAvailable(indexOfOldestMessage: Int, earliestDate: Date?): Boolean {
        throw UnsupportedOperationException("not implemented")
    }

    override fun fetch(
        messages: List<ImapMessage>,
        fetchProfile: FetchProfile,
        listener: FetchListener?,
        maxDownloadSize: Int,
    ) {
        if (messages.isEmpty()) return

        for (imapMessage in messages) {
            val uid = imapMessage.uid.toLong()

            val flags = messageFlags[uid].orEmpty().toSet()
            imapMessage.setFlags(flags, true)

            val storedMessage = this.messages[uid] ?: error("Message $uid not found")
            for (header in storedMessage.headers) {
                imapMessage.addHeader(header.name, header.value)
            }
            imapMessage.body = storedMessage.body

            listener?.onFetchResponse(imapMessage, isFirstResponse = true)
        }
    }

    override fun fetchPart(
        message: ImapMessage,
        part: Part,
        bodyFactory: BodyFactory,
        maxDownloadSize: Int,
    ) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun search(
        queryString: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        performFullTextSearch: Boolean,
    ): List<ImapMessage> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun appendMessages(messages: List<Message>): Map<String, String>? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setFlagsForAllMessages(flags: Set<Flag>, value: Boolean) {
        if (value) {
            for (messageFlagSet in messageFlags.values) {
                messageFlagSet.addAll(flags)
            }
        } else {
            for (messageFlagSet in messageFlags.values) {
                messageFlagSet.removeAll(flags)
            }
        }
    }

    override fun setFlags(messages: List<ImapMessage>, flags: Set<Flag>, value: Boolean) {
        for (message in messages) {
            val uid = message.uid.toLong()
            val messageFlagSet = messageFlags[uid] ?: error("Unknown message with UID $uid")
            if (value) {
                messageFlagSet.addAll(flags)
            } else {
                messageFlagSet.removeAll(flags)
            }
        }
    }

    override fun copyMessages(messages: List<ImapMessage>, folder: ImapFolder): Map<String, String>? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun moveMessages(messages: List<ImapMessage>, folder: ImapFolder): Map<String, String>? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun deleteMessages(messages: List<ImapMessage>) {
        setFlags(messages, setOf(Flag.DELETED), true)
    }

    override fun deleteAllMessages() {
        setFlagsForAllMessages(setOf(Flag.DELETED), true)
    }

    override fun expunge() {
        mode = OpenMode.READ_WRITE
        wasExpunged = true
    }

    override fun expungeUids(uids: List<String>) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun create(folderType: FolderType): Boolean {
        throw UnsupportedOperationException("not implemented")
    }
}
