package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageRetrievalListener
import com.fsck.k9.mail.Part
import java.io.IOException
import java.util.Date
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.common.mail.Flag

interface ImapFolder {
    val serverId: String
    val mode: OpenMode?
    val messageCount: Int
    val isOpen: Boolean

    @Throws(MessagingException::class)
    fun exists(): Boolean

    @Throws(MessagingException::class)
    fun open(mode: OpenMode)

    fun close()

    fun getUidValidity(): Long?

    fun getMessage(uid: String): ImapMessage

    @Throws(MessagingException::class)
    fun getUidFromMessageId(messageId: String): String?

    @Throws(MessagingException::class)
    fun getMessages(
        start: Int,
        end: Int,
        earliestDate: Date?,
        listener: MessageRetrievalListener<ImapMessage>?,
    ): List<ImapMessage>

    @Throws(IOException::class, MessagingException::class)
    fun areMoreMessagesAvailable(indexOfOldestMessage: Int, earliestDate: Date?): Boolean

    @Throws(MessagingException::class)
    fun fetch(
        messages: List<ImapMessage>,
        fetchProfile: FetchProfile,
        listener: FetchListener?,
        maxDownloadSize: Int,
    )

    @Throws(MessagingException::class)
    fun fetchPart(
        message: ImapMessage,
        part: Part,
        bodyFactory: BodyFactory,
        maxDownloadSize: Int,
    )

    @Throws(MessagingException::class)
    fun search(
        queryString: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        performFullTextSearch: Boolean,
    ): List<ImapMessage>

    @Throws(MessagingException::class)
    fun appendMessages(messages: List<Message>): Map<String, String>?

    @Throws(MessagingException::class)
    fun setFlagsForAllMessages(flags: Set<Flag>, value: Boolean)

    @Throws(MessagingException::class)
    fun setFlags(messages: List<ImapMessage>, flags: Set<Flag>, value: Boolean)

    @Throws(MessagingException::class)
    fun copyMessages(messages: List<ImapMessage>, folder: ImapFolder): Map<String, String>?

    @Throws(MessagingException::class)
    fun moveMessages(messages: List<ImapMessage>, folder: ImapFolder): Map<String, String>?

    @Throws(MessagingException::class)
    fun deleteMessages(messages: List<ImapMessage>)

    @Throws(MessagingException::class)
    fun deleteAllMessages()

    @Throws(MessagingException::class)
    fun expunge()

    @Throws(MessagingException::class)
    fun expungeUids(uids: List<String>)

    /**
     * Creates this folder on the IMAP server.
     *
     * @throws MessagingException when fails to create folder on IMAP server.
     */
    @Throws(MessagingException::class)
    fun create(folderType: FolderType = FolderType.REGULAR): Boolean
}

interface FetchListener {
    fun onFetchResponse(message: ImapMessage, isFirstResponse: Boolean)
}
