package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageRetrievalListener
import com.fsck.k9.mail.Part
import java.util.Date

internal open class TestImapFolder(
    override val serverId: String,
    val connection: TestImapConnection,
) : ImapFolder {
    override var mode: OpenMode? = null
        protected set

    override var messageCount: Int = 0
        protected set

    override var isOpen: Boolean = false
        protected set

    private var openAction: () -> Unit = {}

    override fun exists(): Boolean {
        throw UnsupportedOperationException("not implemented")
    }

    override fun open(mode: OpenMode) {
        openAction()
        this.mode = mode
        connection.open()
        isOpen = true
    }

    override fun close() {
        connection.close()
        isOpen = false
        mode = null
    }

    override fun getUidValidity(): Long? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getMessage(uid: String): ImapMessage {
        throw UnsupportedOperationException("not implemented")
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
        throw UnsupportedOperationException("not implemented")
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
        throw UnsupportedOperationException("not implemented")
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
        throw UnsupportedOperationException("not implemented")
    }

    override fun setFlags(messages: List<ImapMessage>, flags: Set<Flag>, value: Boolean) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun copyMessages(messages: List<ImapMessage>, folder: ImapFolder): Map<String, String>? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun moveMessages(messages: List<ImapMessage>, folder: ImapFolder): Map<String, String>? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun deleteMessages(messages: List<ImapMessage>) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun deleteAllMessages() {
        throw UnsupportedOperationException("not implemented")
    }

    override fun expunge() {
        throw UnsupportedOperationException("not implemented")
    }

    override fun expungeUids(uids: List<String>) {
        throw UnsupportedOperationException("not implemented")
    }

    fun throwOnOpen(block: () -> Nothing) {
        openAction = block
    }
}
