package com.fsck.k9.storage.messages

import android.database.sqlite.SQLiteDatabase
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotIn
import assertk.assertions.isNull
import com.fsck.k9.mail.testing.crlf
import com.fsck.k9.mailstore.StorageFilesProvider
import com.fsck.k9.storage.RobolectricTest
import net.thunderbird.feature.account.AccountIdFactory
import okio.buffer
import okio.sink
import okio.source
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class CopyMessageOperationsTest : RobolectricTest() {

    private val accountId = AccountIdFactory.create()
    private val messagePartDirectory = createRandomTempDirectory()
    private lateinit var sqliteDatabase: SQLiteDatabase
    private lateinit var attachmentFileManager: AttachmentFileManager
    private lateinit var copyMessageOperations: CopyMessageOperations

    @Before
    fun setUp() {
        sqliteDatabase = createDatabase()
        val storageFilesProvider = object : StorageFilesProvider {
            override fun getDatabaseFile() = error("Not implemented")
            override fun getAttachmentDirectory() = messagePartDirectory
        }
        val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
        attachmentFileManager = AttachmentFileManager(storageFilesProvider, mock())
        val threadMessageOperations = ThreadMessageOperations()
        copyMessageOperations = CopyMessageOperations(
            lockableDatabase,
            attachmentFileManager,
            threadMessageOperations,
            accountId,
        )
    }

    @After
    fun tearDown() {
        sqliteDatabase.close()
        messagePartDirectory.deleteRecursively()
    }

    @Test
    fun `copy message that is part of a thread`() {
        val sourceMessagePartId1 = sqliteDatabase.createMessagePart(
            seq = 0,
            dataLocation = DataLocation.CHILD_PART_CONTAINS_DATA,
            mimeType = "multipart/mixed",
            boundary = "--boundary",
            header = "Message-ID: <msg0002@domain.example>\nIn-Reply-To: <msg0001@domain.example>\n".crlf(),
        )
        val sourceMessagePartId2 = sqliteDatabase.createMessagePart(
            seq = 1,
            root = sourceMessagePartId1,
            parent = sourceMessagePartId1,
            mimeType = "text/plain",
            dataLocation = DataLocation.IN_DATABASE,
            data = "Text part".toByteArray(),
        )
        val sourceMessagePartId3 = sqliteDatabase.createMessagePart(
            seq = 2,
            root = sourceMessagePartId1,
            parent = sourceMessagePartId1,
            mimeType = "application/octet-stream",
            dataLocation = DataLocation.ON_DISK,
        )
        attachmentFileManager.getAttachmentFile(sourceMessagePartId3).sink().buffer().use { sink ->
            sink.writeUtf8("Part contents")
        }

        val messageId1 = sqliteDatabase.createMessage(
            folderId = 1,
            empty = true,
            messageIdHeader = "<msg0001@domain.example>",
        )
        val messageId2 = sqliteDatabase.createMessage(
            folderId = 1,
            empty = false,
            messageIdHeader = "<msg0002@domain.example>",
            messagePartId = sourceMessagePartId1,
        )
        val messageId3 = sqliteDatabase.createMessage(
            folderId = 1,
            empty = false,
            messageIdHeader = "<msg0003@domain.example>",
        )
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val threadId2 = sqliteDatabase.createThread(messageId2, root = threadId1, parent = threadId1)
        val threadId3 = sqliteDatabase.createThread(messageId3, root = threadId1, parent = threadId2)

        val destinationMessageId = copyMessageOperations.copyMessage(messageId = messageId2, destinationFolderId = 2)

        assertThat(destinationMessageId).isNotIn(setOf(messageId1, messageId2, messageId3))

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(3 + 2)

        val destinationMessageThread = threads.first { it.messageId == destinationMessageId }
        assertThat(destinationMessageThread.id).isNotIn(setOf(threadId1, threadId2, threadId3))
        assertThat(destinationMessageThread.parent).isEqualTo(destinationMessageThread.root)

        val destinationRootThread = threads.first { it.id == destinationMessageThread.root }
        assertThat(destinationRootThread.messageId).isNotIn(setOf(messageId1, messageId2, messageId3))
        assertThat(destinationRootThread.root).isEqualTo(destinationRootThread.id)
        assertThat(destinationRootThread.parent).isNull()

        val messages = sqliteDatabase.readMessages()
        val destinationRootThreadMessage = messages.first { it.id == destinationRootThread.messageId }
        assertThat(destinationRootThreadMessage.empty).isEqualTo(1)
        assertThat(destinationRootThreadMessage.folderId).isEqualTo(2)
        assertThat(destinationRootThreadMessage.messageId).isEqualTo("<msg0001@domain.example>")

        val destinationMessage = messages.first { it.id == destinationMessageThread.messageId }
        val sourceMessage = messages.first { it.id == messageId2 }
        assertThat(destinationMessage).isEqualTo(
            sourceMessage.copy(
                id = destinationMessageId,
                uid = destinationMessage.uid,
                folderId = 2,
                messagePartId = destinationMessage.messagePartId,
                accountId = accountId.asRaw(),
            ),
        )

        val messageParts = sqliteDatabase.readMessageParts()
        assertThat(messageParts).hasSize(3 + 3)

        val sourceMessagePart1 = messageParts.first { it.id == sourceMessagePartId1 }
        val sourceMessagePart2 = messageParts.first { it.id == sourceMessagePartId2 }
        val sourceMessagePart3 = messageParts.first { it.id == sourceMessagePartId3 }
        val destinationMessagePart1 = messageParts.first { it.id == destinationMessage.messagePartId }
        val destinationMessagePart2 = messageParts.first { it.root == destinationMessage.messagePartId && it.seq == 1 }
        val destinationMessagePart3 = messageParts.first { it.root == destinationMessage.messagePartId && it.seq == 2 }
        assertThat(destinationMessagePart1).isNotIn(setOf(sourceMessagePart1, sourceMessagePart2, sourceMessagePart3))
        assertThat(destinationMessagePart1).isEqualTo(
            sourceMessagePart1.copy(
                id = destinationMessagePart1.id,
                root = destinationMessagePart1.id,
                parent = -1,
            ),
        )
        assertThat(destinationMessagePart2).isNotIn(setOf(sourceMessagePart1, sourceMessagePart2, sourceMessagePart3))
        assertThat(destinationMessagePart2).isEqualTo(
            sourceMessagePart2.copy(
                id = destinationMessagePart2.id,
                root = destinationMessagePart1.id,
                parent = destinationMessagePart1.id,
            ),
        )
        assertThat(destinationMessagePart3).isNotIn(setOf(sourceMessagePart1, sourceMessagePart2, sourceMessagePart3))
        assertThat(destinationMessagePart3).isEqualTo(
            sourceMessagePart3.copy(
                id = destinationMessagePart3.id,
                root = destinationMessagePart1.id,
                parent = destinationMessagePart1.id,
            ),
        )

        val files = messagePartDirectory.list()?.toList() ?: emptyList()
        assertThat(files).hasSize(2)

        attachmentFileManager.getAttachmentFile(destinationMessagePart3.id!!).source().buffer().use { source ->
            assertThat(source.readUtf8()).isEqualTo("Part contents")
        }
    }

    @Test
    fun `copy message into an existing thread`() {
        val sourceMessagePartId = sqliteDatabase.createMessagePart(
            header = "Message-ID: <msg0002@domain.example>\nIn-Reply-To: <msg0001@domain.example>\n".crlf(),
            mimeType = "text/plain",
            dataLocation = DataLocation.IN_DATABASE,
            data = "Text part".toByteArray(),
        )
        attachmentFileManager.getAttachmentFile(sourceMessagePartId).sink().buffer().use { sink ->
            sink.writeUtf8("Part contents")
        }

        val sourceMessageId = sqliteDatabase.createMessage(
            folderId = 1,
            empty = false,
            messageIdHeader = "<msg0002@domain.example>",
            messagePartId = sourceMessagePartId,
        )
        val destinationMessageId = sqliteDatabase.createMessage(
            folderId = 2,
            empty = true,
            messageIdHeader = "<msg0002@domain.example>",
        )
        val otherDestinationMessageId = sqliteDatabase.createMessage(
            folderId = 2,
            empty = false,
            messageIdHeader = "<msg0003@domain.example>",
        )
        val destinationThreadId = sqliteDatabase.createThread(destinationMessageId)
        val otherDestinationThreadId = sqliteDatabase.createThread(
            otherDestinationMessageId,
            root = destinationThreadId,
            parent = destinationThreadId,
        )

        val resultMessageId = copyMessageOperations.copyMessage(messageId = sourceMessageId, destinationFolderId = 2)

        assertThat(resultMessageId).isEqualTo(destinationMessageId)

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(2 + 1)

        val destinationThread = threads.first { it.messageId == destinationMessageId }
        assertThat(destinationThread.id).isEqualTo(destinationThreadId)
        assertThat(destinationThread.parent).isEqualTo(destinationThread.root)

        val destinationRootThread = threads.first { it.id == destinationThread.root }
        assertThat(destinationRootThread.root).isEqualTo(destinationRootThread.id)
        assertThat(destinationRootThread.parent).isNull()

        val otherDestinationThread = threads.first { it.id == otherDestinationThreadId }
        assertThat(otherDestinationThread.root).isEqualTo(destinationRootThread.id)
        assertThat(otherDestinationThread.parent).isEqualTo(destinationThread.id)

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(3 + 1)

        val destinationRootThreadMessage = messages.first { it.id == destinationRootThread.messageId }
        assertThat(destinationRootThreadMessage.empty).isEqualTo(1)
        assertThat(destinationRootThreadMessage.folderId).isEqualTo(2)
        assertThat(destinationRootThreadMessage.messageId).isEqualTo("<msg0001@domain.example>")

        val destinationMessage = messages.first { it.id == destinationMessageId }
        val sourceMessage = messages.first { it.id == sourceMessageId }
        assertThat(destinationMessage).isEqualTo(
            sourceMessage.copy(
                id = destinationMessageId,
                uid = destinationMessage.uid,
                folderId = 2,
                messagePartId = destinationMessage.messagePartId,
                accountId = accountId.asRaw(),
            ),
        )

        val messageParts = sqliteDatabase.readMessageParts()
        assertThat(messageParts).hasSize(1 + 1)

        val sourceMessagePart = messageParts.first { it.id == sourceMessagePartId }
        val destinationMessagePart = messageParts.first { it.id == destinationMessage.messagePartId }
        assertThat(destinationMessagePart).isEqualTo(
            sourceMessagePart.copy(
                id = destinationMessagePart.id,
                root = destinationMessagePart.id,
                parent = -1,
            ),
        )
    }
}
