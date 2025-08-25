package com.fsck.k9.storage.messages

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fsck.k9.storage.RobolectricTest
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.Before
import org.junit.Test

class ThreadMessageOperationsTest : RobolectricTest() {
    private val sqliteDatabase = createDatabase()
    private val threadMessageOperations = ThreadMessageOperations()

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Test
    fun `insert message without existing thread`() {
        val threadInfo = threadMessageOperations.doMessageThreading(
            sqliteDatabase,
            folderId = 1,
            ThreadHeaders(messageIdHeader = "<msg001@domain.example>", inReplyToHeader = null, referencesHeader = null),
        )

        assertThat(threadInfo).isNull()

        assertThat(sqliteDatabase.readThreads()).isEmpty()
    }

    @Test
    fun `insert message with in-reply-to header without existing thread`() {
        val threadInfo = threadMessageOperations.doMessageThreading(
            sqliteDatabase,
            folderId = 1,
            ThreadHeaders(
                messageIdHeader = "<msg002@domain.example>",
                inReplyToHeader = "<msg001@domain.example>",
                referencesHeader = null,
            ),
        )

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(1)

        val rootThread = threads.first { it.id == it.root }
        assertThat(rootThread.parent).isNull()

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(1)

        val emptyMessage = messages.first()
        assertThat(emptyMessage.id).isEqualTo(rootThread.messageId)
        assertThat(emptyMessage.empty).isEqualTo(1)
        assertThat(emptyMessage.messageId).isEqualTo("<msg001@domain.example>")

        assertThat(threadInfo).isEqualTo(
            ThreadInfo(
                threadId = null,
                messageId = null,
                rootId = rootThread.id!!,
                parentId = rootThread.id,
            ),
        )
    }

    @Test
    fun `insert message with in-reply-to header into existing thread`() {
        val messageId = sqliteDatabase.createMessage(
            folderId = 1,
            empty = false,
            messageIdHeader = "<msg001@domain.example>",
        )
        val threadId = sqliteDatabase.createThread(messageId)

        val threadInfo = threadMessageOperations.doMessageThreading(
            sqliteDatabase,
            folderId = 1,
            ThreadHeaders(
                messageIdHeader = "<msg002@domain.example>",
                inReplyToHeader = "<msg001@domain.example>",
                referencesHeader = null,
            ),
        )

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(1)

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(1)

        assertThat(threadInfo).isEqualTo(
            ThreadInfo(
                threadId = null,
                messageId = null,
                rootId = threadId,
                parentId = threadId,
            ),
        )
    }

    @Test
    fun `insert message into existing thread as thread root`() {
        val messageId1 = sqliteDatabase.createMessage(
            folderId = 1,
            empty = true,
            messageIdHeader = "<msg001@domain.example>",
        )
        val messageId2 = sqliteDatabase.createMessage(
            folderId = 1,
            empty = false,
            messageIdHeader = "<msg002@domain.example>",
        )
        val threadId1 = sqliteDatabase.createThread(messageId1)
        sqliteDatabase.createThread(messageId2, root = threadId1, parent = threadId1)

        val threadInfo = threadMessageOperations.doMessageThreading(
            sqliteDatabase,
            folderId = 1,
            ThreadHeaders(
                messageIdHeader = "<msg001@domain.example>",
                inReplyToHeader = null,
                referencesHeader = null,
            ),
        )

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(2)

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(2)

        assertThat(threadInfo).isEqualTo(
            ThreadInfo(
                threadId = threadId1,
                messageId = messageId1,
                rootId = threadId1,
                parentId = null,
            ),
        )
    }

    @Test
    fun `insert message into existing thread changing the thread root`() {
        val messageId1 = sqliteDatabase.createMessage(
            folderId = 1,
            messageIdHeader = "<msg001@domain.example>",
            empty = true,
        )
        val messageId2 = sqliteDatabase.createMessage(
            folderId = 1,
            messageIdHeader = "<msg002@domain.example>",
            empty = true,
        )
        val messageId3 = sqliteDatabase.createMessage(
            folderId = 1,
            messageIdHeader = "<msg003@domain.example>",
            empty = false,
        )
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val threadId2 = sqliteDatabase.createThread(messageId2, root = threadId1, parent = threadId1)
        val threadId3 = sqliteDatabase.createThread(messageId3, root = threadId1, parent = threadId2)

        val threadInfo = threadMessageOperations.doMessageThreading(
            sqliteDatabase,
            folderId = 1,
            ThreadHeaders(
                messageIdHeader = "<msg002@domain.example>",
                inReplyToHeader = "<msg001@domain.example>",
                referencesHeader = "<msg000@domain.example> <msg001@domain.example>",
            ),
        )

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(4)

        val newRootThread = threads.first { it.id !in setOf(threadId1, threadId2, threadId3) }
        assertThat(newRootThread.root).isEqualTo(newRootThread.id)
        assertThat(newRootThread.parent).isNull()

        for (thread in threads) {
            assertThat(thread.root).isEqualTo(newRootThread.id)
        }

        assertThat(threadInfo).isEqualTo(
            ThreadInfo(
                threadId = threadId2,
                messageId = messageId2,
                rootId = newRootThread.id!!,
                parentId = threadId1,
            ),
        )
    }

    @Test
    fun `merge two existing threads`() {
        val messageIdB = sqliteDatabase.createMessage(
            folderId = 1,
            messageIdHeader = "<msgB@domain.example>",
            empty = true,
        )
        val messageIdC = sqliteDatabase.createMessage(
            folderId = 1,
            messageIdHeader = "<msgC@domain.example>",
            empty = false,
        )
        val messageIdD = sqliteDatabase.createMessage(
            folderId = 1,
            messageIdHeader = "<msgD@domain.example>",
            empty = true,
        )
        val messageIdE = sqliteDatabase.createMessage(
            folderId = 1,
            messageIdHeader = "<msgE@domain.example>",
            empty = false,
        )
        val threadIdB = sqliteDatabase.createThread(messageIdB)
        val threadIdC = sqliteDatabase.createThread(messageIdC, root = threadIdB, parent = threadIdB)
        val threadIdD = sqliteDatabase.createThread(messageIdD)
        val threadIdE = sqliteDatabase.createThread(messageIdE, root = threadIdD, parent = threadIdD)

        val threadInfo = threadMessageOperations.doMessageThreading(
            sqliteDatabase,
            folderId = 1,
            ThreadHeaders(
                messageIdHeader = "<msgD@domain.example>",
                inReplyToHeader = "<msgC@domain.example>",
                referencesHeader = "<msgA@domain.example>",
            ),
        )

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(4 + 1)

        val threadRoots = threads.filter { it.root == it.id }
        assertThat(threadRoots).hasSize(1)

        val threadA = threadRoots.first()
        for (thread in threads) {
            assertThat(thread.root).isEqualTo(threadA.id)
        }

        val messages = sqliteDatabase.readMessages()
        val messageA = messages.first { it.id == threadA.messageId }
        assertThat(messageA.empty).isEqualTo(1)
        assertThat(messageA.messageId).isEqualTo("<msgA@domain.example>")

        val threadB = threads.first { it.id == threadIdB }
        assertThat(threadB.parent).isEqualTo(threadA.id)

        val threadC = threads.first { it.id == threadIdC }
        assertThat(threadC.parent).isEqualTo(threadB.id)

        val threadD = threads.first { it.id == threadIdD }
        assertThat(threadD.parent).isEqualTo(threadC.id)

        val threadE = threads.first { it.id == threadIdE }
        assertThat(threadE.parent).isEqualTo(threadD.id)

        assertThat(threadInfo).isEqualTo(
            ThreadInfo(
                threadId = threadIdD,
                messageId = messageIdD,
                rootId = threadA.id!!,
                parentId = threadIdC,
            ),
        )
    }
}
