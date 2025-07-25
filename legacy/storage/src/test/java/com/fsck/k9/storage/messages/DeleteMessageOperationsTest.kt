package com.fsck.k9.storage.messages

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.extracting
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.fsck.k9.mailstore.StorageFilesProvider
import com.fsck.k9.storage.RobolectricTest
import org.junit.After
import org.junit.Test
import org.mockito.kotlin.mock

class DeleteMessageOperationsTest : RobolectricTest() {
    private val messagePartDirectory = createRandomTempDirectory()
    private val sqliteDatabase = createDatabase()
    private val storageFilesProvider = object : StorageFilesProvider {
        override fun getDatabaseFile() = error("Not implemented")
        override fun getAttachmentDirectory() = messagePartDirectory
    }
    private val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
    private val attachmentFileManager = AttachmentFileManager(storageFilesProvider, mock())
    private val deleteMessageOperations = DeleteMessageOperations(lockableDatabase, attachmentFileManager)

    @After
    fun tearDown() {
        messagePartDirectory.deleteRecursively()
    }

    @Test
    fun `destroy message with empty parent messages`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(
            folderId = folderId,
            uid = "empty1",
            empty = true,
            messageIdHeader = "msg001@domain.example",
        )
        val messageId2 = sqliteDatabase.createMessage(
            folderId = folderId,
            uid = "empty2",
            empty = true,
            messageIdHeader = "msg002@domain.example",
        )
        val messageId3 = sqliteDatabase.createMessage(
            folderId = folderId,
            uid = "delete",
            empty = false,
            messageIdHeader = "msg003@domain.example",
        )
        val threadId1 = sqliteDatabase.createThread(messageId = messageId1)
        val threadId2 = sqliteDatabase.createThread(messageId = messageId2, root = threadId1, parent = threadId1)
        sqliteDatabase.createThread(messageId = messageId3, root = threadId1, parent = threadId2)

        deleteMessageOperations.destroyMessages(folderId = folderId, messageServerIds = listOf("delete"))

        assertThat(sqliteDatabase.readMessages()).isEmpty()
        assertThat(sqliteDatabase.readThreads()).isEmpty()
    }

    @Test
    fun `destroy message with empty parent message that has another child`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(
            folderId = folderId,
            uid = "empty",
            empty = true,
            messageIdHeader = "msg001@domain.example",
        )
        val messageId2 = sqliteDatabase.createMessage(
            folderId = folderId,
            uid = "child1",
            empty = false,
            messageIdHeader = "msg002@domain.example",
        )
        val messageId3 = sqliteDatabase.createMessage(
            folderId = folderId,
            uid = "delete",
            empty = false,
            messageIdHeader = "msg003@domain.example",
        )
        val threadId1 = sqliteDatabase.createThread(messageId = messageId1)
        val threadId2 = sqliteDatabase.createThread(messageId = messageId2, root = threadId1, parent = threadId1)
        sqliteDatabase.createThread(messageId = messageId3, root = threadId1, parent = threadId1)

        deleteMessageOperations.destroyMessages(folderId = folderId, messageServerIds = listOf("delete"))

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(2)
        assertThat(messages.map { it.id }.toSet()).isEqualTo(setOf(messageId1, messageId2))

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(2)
        assertThat(threads.map { it.id }.toSet()).isEqualTo(setOf(threadId1, threadId2))
    }

    @Test
    fun `destroy message with non-empty parent message`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(
            folderId = folderId,
            uid = "parent",
            empty = false,
            messageIdHeader = "msg001@domain.example",
        )
        val messageId2 = sqliteDatabase.createMessage(
            folderId = folderId,
            uid = "delete",
            empty = false,
            messageIdHeader = "msg002@domain.example",
        )
        val threadId1 = sqliteDatabase.createThread(messageId = messageId1)
        sqliteDatabase.createThread(messageId = messageId2, root = threadId1, parent = threadId1)

        deleteMessageOperations.destroyMessages(folderId = folderId, messageServerIds = listOf("delete"))

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(1)
        assertThat(messages.first().id).isEqualTo(messageId1)

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(1)
        assertThat(threads.first().id).isEqualTo(threadId1)
    }

    @Test
    fun `destroy message without parent message`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(
            folderId = folderId,
            uid = "delete",
            empty = false,
            messageIdHeader = "msg001@domain.example",
        )
        sqliteDatabase.createThread(messageId = messageId1)

        deleteMessageOperations.destroyMessages(folderId = folderId, messageServerIds = listOf("delete"))

        assertThat(sqliteDatabase.readMessages()).isEmpty()
        assertThat(sqliteDatabase.readThreads()).isEmpty()
    }

    @Test
    fun `destroy message with child message should convert to empty message`() {
        val folderId = sqliteDatabase.createFolder()
        val messageId1 = sqliteDatabase.createMessage(
            folderId = folderId,
            uid = "delete",
            empty = false,
            messageIdHeader = "msg001@domain.example",
        )
        val messageId2 = sqliteDatabase.createMessage(
            folderId = folderId,
            uid = "child",
            empty = false,
            messageIdHeader = "msg002@domain.example",
        )
        val threadId1 = sqliteDatabase.createThread(messageId = messageId1)
        val threadId2 = sqliteDatabase.createThread(messageId = messageId2, root = threadId1, parent = threadId1)

        deleteMessageOperations.destroyMessages(folderId = folderId, messageServerIds = listOf("delete"))

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(2)
        val message1 = messages.first { it.id == messageId1 }
        assertThat(message1.empty).isEqualTo(1)
        assertThat(message1.subject).isNull()
        assertThat(message1.date).isNull()
        assertThat(message1.flags).isNull()
        assertThat(message1.senderList).isNull()
        assertThat(message1.toList).isNull()
        assertThat(message1.ccList).isNull()
        assertThat(message1.bccList).isNull()
        assertThat(message1.replyToList).isNull()
        assertThat(message1.attachmentCount).isNull()
        assertThat(message1.internalDate).isNull()
        assertThat(message1.previewType).isEqualTo("none")
        assertThat(message1.preview).isNull()
        assertThat(message1.mimeType).isNull()
        assertThat(message1.normalizedSubjectHash).isNull()
        assertThat(message1.messagePartId).isNull()
        assertThat(message1.encryptionType).isNull()
        assertThat(messages.firstOrNull { it.id == messageId2 }).isNotNull()

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(2)
        assertThat(threads).extracting { it.id }.containsExactlyInAnyOrder(threadId1, threadId2)
    }
}
