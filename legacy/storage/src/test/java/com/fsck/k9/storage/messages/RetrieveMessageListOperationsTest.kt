package com.fsck.k9.storage.messages

import android.database.sqlite.SQLiteDatabase
import app.k9mail.legacy.mailstore.MessageMapper
import app.k9mail.legacy.message.extractors.PreviewResult.PreviewType
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fsck.k9.mail.Address
import com.fsck.k9.mailstore.DatabasePreviewType
import com.fsck.k9.storage.RobolectricTest
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.After
import org.junit.Before
import org.junit.Test

class RetrieveMessageListOperationsTest : RobolectricTest() {
    private lateinit var sqliteDatabase: SQLiteDatabase
    private lateinit var retrieveMessageListOperations: RetrieveMessageListOperations

    @Before
    fun setUp() {
        Log.logger = TestLogger()
        sqliteDatabase = createDatabase()
        val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
        retrieveMessageListOperations = RetrieveMessageListOperations(lockableDatabase)
    }

    @After
    fun tearDown() {
        sqliteDatabase.close()
    }

    @Test
    fun `getMessages() on empty folder`() {
        val folderId = sqliteDatabase.createFolder()

        val result = getMessagesFromFolder(folderId) { "unexpected" }

        assertThat(result).isEmpty()
    }

    @Test
    fun `getMessages() with only a deleted message`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId = sqliteDatabase.createMessage(folderId, uid = "uid1", deleted = true)
        sqliteDatabase.createThread(messageId)

        val result = getMessagesFromFolder(folderId) { "unexpected" }

        assertThat(result).isEmpty()
    }

    @Test
    fun `getMessages() with single message`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId = sqliteDatabase.createMessage(
            folderId,
            uid = "uid1",
            subject = "subject",
            date = 123L,
            senderList = Address.pack(Address.parse("from@domain.example")),
            toList = Address.pack(Address.parse("to@domain.example")),
            ccList = Address.pack(Address.parse("cc1@domain.example, cc2@domain.example")),
            attachmentCount = 1,
            internalDate = 456L,
            previewType = DatabasePreviewType.TEXT,
            preview = "preview",
            read = true,
            flagged = true,
            answered = true,
            forwarded = true,
        )
        val threadId = sqliteDatabase.createThread(messageId)

        val result = getMessagesFromFolder(folderId) { message ->
            assertThat(message.id).isEqualTo(messageId)
            assertThat(message.messageServerId).isEqualTo("uid1")
            assertThat(message.folderId).isEqualTo(folderId)
            assertThat(message.fromAddresses).containsExactly(Address("from@domain.example"))
            assertThat(message.toAddresses).containsExactly(Address("to@domain.example"))
            assertThat(message.ccAddresses).containsExactly(
                Address("cc1@domain.example"),
                Address("cc2@domain.example"),
            )
            assertThat(message.messageDate).isEqualTo(123L)
            assertThat(message.internalDate).isEqualTo(456L)
            assertThat(message.subject).isEqualTo("subject")
            assertThat(message.preview.previewType).isEqualTo(PreviewType.TEXT)
            assertThat(message.preview.previewText).isEqualTo("preview")
            assertThat(message.isRead).isTrue()
            assertThat(message.isStarred).isTrue()
            assertThat(message.isAnswered).isTrue()
            assertThat(message.isForwarded).isTrue()
            assertThat(message.hasAttachments).isTrue()
            assertThat(message.threadRoot).isEqualTo(threadId)
            assertThat(message.threadCount).isEqualTo(0)
            "OK"
        }

        assertThat(result).containsExactly("OK")
    }

    @Test
    fun `getMessages() with folder containing an empty message`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(folderId, empty = true)
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(folderId, uid = "uid2")
        sqliteDatabase.createThread(messageId2, root = threadId1)

        val result = getThreadedMessagesFromFolder(folderId) { message -> message.id }

        assertThat(result).containsExactly(messageId2)
    }

    @Test
    fun `getMessages() with folder containing a deleted message`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(folderId, deleted = true)
        sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(folderId, uid = "uid2")
        sqliteDatabase.createThread(messageId2)

        val result = getThreadedMessagesFromFolder(folderId) { message -> message.id }

        assertThat(result).containsExactly(messageId2)
    }

    @Test
    fun `getMessages() selecting only unread messages`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(folderId, uid = "uid1", read = false)
        sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(folderId, uid = "uid2", read = true)
        sqliteDatabase.createThread(messageId2)
        val messageId3 = sqliteDatabase.createMessage(folderId, uid = "uid3", read = false)
        sqliteDatabase.createThread(messageId3)

        val result = retrieveMessageListOperations.getThreadedMessages(
            selection = "folder_id = ? AND read = 0",
            selectionArgs = arrayOf(folderId.toString()),
            sortOrder = "date DESC, id DESC",
        ) { message ->
            message.id
        }

        assertThat(result).containsExactly(messageId3, messageId1)
    }

    @Test
    fun `getThreadedMessages() on empty folder`() {
        val folderId = sqliteDatabase.createFolder()

        val result = getThreadedMessagesFromFolder(folderId) { "unexpected" }

        assertThat(result).isEmpty()
    }

    @Test
    fun `getThreadedMessages() with only a deleted message`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId = sqliteDatabase.createMessage(folderId, uid = "uid1", deleted = true)
        sqliteDatabase.createThread(messageId)

        val result = getThreadedMessagesFromFolder(folderId) { "unexpected" }

        assertThat(result).isEmpty()
    }

    @Test
    fun `getThreadedMessages() with single message`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId = sqliteDatabase.createMessage(
            folderId,
            uid = "uid1",
            subject = "subject",
            date = 123L,
            senderList = Address.pack(Address.parse("from@domain.example")),
            toList = Address.pack(Address.parse("to@domain.example")),
            ccList = Address.pack(Address.parse("cc1@domain.example, cc2@domain.example")),
            attachmentCount = 1,
            internalDate = 456L,
            previewType = DatabasePreviewType.TEXT,
            preview = "preview",
            read = true,
            flagged = true,
            answered = true,
            forwarded = true,
        )
        val threadId = sqliteDatabase.createThread(messageId)

        val result = getThreadedMessagesFromFolder(folderId) { message ->
            assertThat(message.id).isEqualTo(messageId)
            assertThat(message.messageServerId).isEqualTo("uid1")
            assertThat(message.folderId).isEqualTo(folderId)
            assertThat(message.fromAddresses).containsExactly(Address("from@domain.example"))
            assertThat(message.toAddresses).containsExactly(Address("to@domain.example"))
            assertThat(message.ccAddresses).containsExactly(
                Address("cc1@domain.example"),
                Address("cc2@domain.example"),
            )
            assertThat(message.messageDate).isEqualTo(123L)
            assertThat(message.internalDate).isEqualTo(456L)
            assertThat(message.subject).isEqualTo("subject")
            assertThat(message.preview.previewType).isEqualTo(PreviewType.TEXT)
            assertThat(message.preview.previewText).isEqualTo("preview")
            assertThat(message.isRead).isTrue()
            assertThat(message.isStarred).isTrue()
            assertThat(message.isAnswered).isTrue()
            assertThat(message.isForwarded).isTrue()
            assertThat(message.hasAttachments).isTrue()
            assertThat(message.threadRoot).isEqualTo(threadId)
            assertThat(message.threadCount).isEqualTo(1)
            "OK"
        }

        assertThat(result).containsExactly("OK")
    }

    @Test
    fun `getThreadedMessages() with thread containing an empty message`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(folderId, empty = true)
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(folderId, uid = "uid2")
        sqliteDatabase.createThread(messageId2, root = threadId1)

        val result = getThreadedMessagesFromFolder(folderId) { message ->
            assertThat(message.id).isEqualTo(messageId2)
            assertThat(message.messageServerId).isEqualTo("uid2")
            assertThat(message.threadRoot).isEqualTo(threadId1)
            "OK"
        }

        assertThat(result).containsExactly("OK")
    }

    @Test
    fun `getThreadedMessages() should return latest message in thread`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(
            folderId,
            uid = "uid1",
            date = 1000L,
            internalDate = 1001L,
        )
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(
            folderId,
            uid = "uid2",
            date = 2000L,
            internalDate = 2001L,
        )
        sqliteDatabase.createThread(messageId2, root = threadId1)

        val result = getThreadedMessagesFromFolder(folderId) { message ->
            assertThat(message.id).isEqualTo(messageId2)
            assertThat(message.messageDate).isEqualTo(2000L)
            assertThat(message.internalDate).isEqualTo(2001L)
            "OK"
        }

        assertThat(result).containsExactly("OK")
    }

    @Test
    fun `getThreadedMessages() should return 'unread' when at least one message in thread is marked as unread`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(folderId, uid = "uid1", read = true)
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(folderId, uid = "uid2", read = false)
        sqliteDatabase.createThread(messageId2, root = threadId1)

        val result = getThreadedMessagesFromFolder(folderId) { message ->
            assertThat(message.isRead).isFalse()
            "OK"
        }

        assertThat(result).containsExactly("OK")
    }

    @Test
    fun `getThreadedMessages() should return 'read' when all messages in thread are marked as read`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(folderId, uid = "uid1", read = true)
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(folderId, uid = "uid2", read = true)
        sqliteDatabase.createThread(messageId2, root = threadId1)

        val result = getThreadedMessagesFromFolder(folderId) { message ->
            assertThat(message.isRead).isTrue()
            "OK"
        }

        assertThat(result).containsExactly("OK")
    }

    @Test
    fun `getThreadedMessages() should return 'starred' when at least one message in thread is marked as starred`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(folderId, uid = "uid1", flagged = false)
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(folderId, uid = "uid2", flagged = true)
        sqliteDatabase.createThread(messageId2, root = threadId1)

        val result = getThreadedMessagesFromFolder(folderId) { message ->
            assertThat(message.isStarred).isTrue()
            "OK"
        }

        assertThat(result).containsExactly("OK")
    }

    @Test
    fun `getThreadedMessages() should return 'not starred' when all messages in thread are not marked as starred`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(folderId, uid = "uid1", flagged = false)
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(folderId, uid = "uid2", flagged = false)
        sqliteDatabase.createThread(messageId2, root = threadId1)

        val result = getThreadedMessagesFromFolder(folderId) { message ->
            assertThat(message.isStarred).isFalse()
            "OK"
        }

        assertThat(result).containsExactly("OK")
    }

    @Test
    fun `getThreadedMessages() should return 'not answered' when not all messages in thread are marked as answered`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(folderId, uid = "uid1", answered = false)
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(folderId, uid = "uid2", answered = true)
        sqliteDatabase.createThread(messageId2, root = threadId1)

        val result = getThreadedMessagesFromFolder(folderId) { message ->
            assertThat(message.isAnswered).isFalse()
            "OK"
        }

        assertThat(result).containsExactly("OK")
    }

    @Test
    fun `getThreadedMessages() should return 'answered' when all messages in thread are marked as answered`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(folderId, uid = "uid1", answered = true)
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(folderId, uid = "uid2", answered = true)
        sqliteDatabase.createThread(messageId2, root = threadId1)

        val result = getThreadedMessagesFromFolder(folderId) { message ->
            assertThat(message.isAnswered).isTrue()
            "OK"
        }

        assertThat(result).containsExactly("OK")
    }

    @Test
    fun `getThreadedMessages() should return 'not forwarded' when not all messages in thread are marked as forwarded`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(folderId, uid = "uid1", forwarded = true)
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(folderId, uid = "uid2", forwarded = false)
        sqliteDatabase.createThread(messageId2, root = threadId1)

        val result = getThreadedMessagesFromFolder(folderId) { message ->
            assertThat(message.isForwarded).isFalse()
            "OK"
        }

        assertThat(result).containsExactly("OK")
    }

    @Test
    fun `getThreadedMessages() should return 'forwarded' when all messages in thread are marked as forwarded`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(folderId, uid = "uid1", forwarded = true)
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(folderId, uid = "uid2", forwarded = true)
        sqliteDatabase.createThread(messageId2, root = threadId1)

        val result = getThreadedMessagesFromFolder(folderId) { message ->
            assertThat(message.isForwarded).isTrue()
            "OK"
        }

        assertThat(result).containsExactly("OK")
    }

    @Test
    fun `getThreadedMessages() should return 'has attachment' when at least one message in thread contains an attachment`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(folderId, uid = "uid1", attachmentCount = 1)
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(folderId, uid = "uid2", attachmentCount = 0)
        sqliteDatabase.createThread(messageId2, root = threadId1)

        val result = getThreadedMessagesFromFolder(folderId) { message ->
            assertThat(message.hasAttachments).isTrue()
            "OK"
        }

        assertThat(result).containsExactly("OK")
    }

    @Test
    fun `getThreadedMessages() should return 'has no attachment' when no message in thread contains an attachment`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(folderId, uid = "uid1", attachmentCount = 0)
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(folderId, uid = "uid2", attachmentCount = 0)
        sqliteDatabase.createThread(messageId2, root = threadId1)

        val result = getThreadedMessagesFromFolder(folderId) { message ->
            assertThat(message.hasAttachments).isFalse()
            "OK"
        }

        assertThat(result).containsExactly("OK")
    }

    @Test
    fun `getThreadedMessages() with 3 messages in thread`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(folderId, uid = "uid1")
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(folderId, uid = "uid2")
        sqliteDatabase.createThread(messageId2, root = threadId1)
        val messageId3 = sqliteDatabase.createMessage(folderId, uid = "uid3")
        sqliteDatabase.createThread(messageId3, root = threadId1)

        val result = getThreadedMessagesFromFolder(folderId) { message ->
            assertThat(message.threadCount).isEqualTo(3)
            "OK"
        }

        assertThat(result).containsExactly("OK")
    }

    @Test
    fun `getThreadedMessages() should not include empty messages in thread count`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(folderId, uid = "uid1", empty = true)
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(folderId, uid = "uid2")
        sqliteDatabase.createThread(messageId2, root = threadId1)
        val messageId3 = sqliteDatabase.createMessage(folderId, uid = "uid3")
        sqliteDatabase.createThread(messageId3, root = threadId1)

        val result = getThreadedMessagesFromFolder(folderId) { message ->
            assertThat(message.threadCount).isEqualTo(2)
            "OK"
        }

        assertThat(result).containsExactly("OK")
    }

    @Test
    fun `getThread() with empty message as thread root`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(folderId, empty = true)
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(folderId, uid = "uid2")
        sqliteDatabase.createThread(messageId2, root = threadId1)
        val messageId3 = sqliteDatabase.createMessage(folderId, uid = "uid3")
        sqliteDatabase.createThread(messageId3, root = threadId1)

        val result = retrieveMessageListOperations.getThread(threadId = threadId1, sortOrder = "date DESC") { it.id }

        assertThat(result).containsExactly(messageId2, messageId3)
    }

    @Test
    fun `getThread() should only return messages in thread`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(folderId, uid = "uid1")
        sqliteDatabase.createThread(messageId1)
        val messageId2 = sqliteDatabase.createMessage(folderId, uid = "uid2")
        val threadId2 = sqliteDatabase.createThread(messageId2)
        val messageId3 = sqliteDatabase.createMessage(folderId, uid = "uid3")
        sqliteDatabase.createThread(messageId3, root = threadId2)

        val result = retrieveMessageListOperations.getThread(threadId = threadId2, sortOrder = "date DESC") { it.id }

        assertThat(result).containsExactly(messageId2, messageId3)
    }

    private fun <T> getMessagesFromFolder(folderId: Long, mapper: MessageMapper<T?>): List<T> {
        return retrieveMessageListOperations.getMessages(
            selection = "folder_id = ?",
            selectionArgs = arrayOf(folderId.toString()),
            sortOrder = "date DESC, id DESC",
            mapper,
        )
    }

    private fun <T> getThreadedMessagesFromFolder(folderId: Long, mapper: MessageMapper<T?>): List<T> {
        return retrieveMessageListOperations.getThreadedMessages(
            selection = "folder_id = ?",
            selectionArgs = arrayOf(folderId.toString()),
            sortOrder = "date DESC, id DESC",
            mapper,
        )
    }
}
