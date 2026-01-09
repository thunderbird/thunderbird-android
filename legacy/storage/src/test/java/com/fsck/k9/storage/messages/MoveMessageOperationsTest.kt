package com.fsck.k9.storage.messages

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.startsWith
import com.fsck.k9.K9
import com.fsck.k9.storage.RobolectricTest
import net.thunderbird.feature.account.AccountIdFactory
import org.junit.Test

private const val SOURCE_FOLDER_ID = 3L
private const val DESTINATION_FOLDER_ID = 23L
private const val MESSAGE_ID_HEADER = "<00000000-0000-4000-0000-000000000000@domain.example>"

class MoveMessageOperationsTest : RobolectricTest() {

    private val accountId = AccountIdFactory.create()
    private val sqliteDatabase = createDatabase()
    private val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
    private val moveMessageOperations = MoveMessageOperations(
        lockableDatabase,
        ThreadMessageOperations(),
        accountId,
    )

    @Test
    fun `move message not part of a thread`() {
        val originalMessageId = sqliteDatabase.createMessage(
            folderId = SOURCE_FOLDER_ID,
            uid = "uid1",
            subject = "Move me",
            messageIdHeader = MESSAGE_ID_HEADER,
        )
        sqliteDatabase.createThread(messageId = originalMessageId)
        val originalMessage = sqliteDatabase.readMessages().first()

        val destinationMessageId = moveMessageOperations.moveMessage(
            messageId = originalMessageId,
            destinationFolderId = DESTINATION_FOLDER_ID,
        )

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(2)

        val sourceMessage = messages.first { it.id == originalMessageId }
        assertThat(sourceMessage.folderId).isEqualTo(SOURCE_FOLDER_ID)
        assertThat(sourceMessage.uid).isEqualTo("uid1")
        assertThat(sourceMessage.messageId).isEqualTo(MESSAGE_ID_HEADER)
        assertPlaceholderEntry(sourceMessage)

        val destinationMessage = messages.first { it.id == destinationMessageId }
        assertThat(destinationMessage.uid).isNotNull().startsWith(K9.LOCAL_UID_PREFIX)
        assertThat(destinationMessage).isEqualTo(
            originalMessage.copy(
                id = destinationMessageId,
                folderId = DESTINATION_FOLDER_ID,
                uid = destinationMessage.uid,
                deleted = 0,
                empty = 0,
                accountId = accountId.toString(),
            ),
        )

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(2)

        val originalMessageThread = threads.first { it.messageId == originalMessageId }
        assertThat(originalMessageThread.id).isEqualTo(originalMessageThread.root)
        assertThat(originalMessageThread.parent).isNull()

        val destinationMessageThread = threads.first { it.messageId == destinationMessageId }
        assertThat(destinationMessageThread.id).isEqualTo(destinationMessageThread.root)
        assertThat(destinationMessageThread.parent).isNull()
    }

    @Test
    fun `move message when destination has empty message entry`() {
        val originalMessageId = sqliteDatabase.createMessage(
            folderId = SOURCE_FOLDER_ID,
            uid = "uid1",
            subject = "Move me",
            messageIdHeader = MESSAGE_ID_HEADER,
            read = false,
        )
        sqliteDatabase.createThread(messageId = originalMessageId)
        val originalMessage = sqliteDatabase.readMessages().first()
        val placeholderMessageId = sqliteDatabase.createMessage(
            empty = true,
            folderId = DESTINATION_FOLDER_ID,
            messageIdHeader = MESSAGE_ID_HEADER,
            uid = "",
        )
        val placeholderThreadId = sqliteDatabase.createThread(messageId = placeholderMessageId)
        val childMessageId = sqliteDatabase.createMessage(
            folderId = DESTINATION_FOLDER_ID,
            messageIdHeader = "<msg02@domain.example>",
            uid = "uid2",
        )
        sqliteDatabase.createThread(
            messageId = childMessageId,
            root = placeholderThreadId,
            parent = placeholderThreadId,
        )

        val destinationMessageId = moveMessageOperations.moveMessage(
            messageId = originalMessageId,
            destinationFolderId = DESTINATION_FOLDER_ID,
        )

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(3)

        val sourceMessage = messages.first { it.id == originalMessageId }
        assertThat(sourceMessage.folderId).isEqualTo(SOURCE_FOLDER_ID)
        assertThat(sourceMessage.uid).isEqualTo("uid1")
        assertThat(sourceMessage.messageId).isEqualTo(MESSAGE_ID_HEADER)
        assertPlaceholderEntry(sourceMessage)

        val destinationMessage = messages.first { it.id == destinationMessageId }
        assertThat(destinationMessage.uid).isNotNull().startsWith(K9.LOCAL_UID_PREFIX)
        assertThat(destinationMessage).isEqualTo(
            originalMessage.copy(
                id = destinationMessageId,
                folderId = DESTINATION_FOLDER_ID,
                uid = destinationMessage.uid,
                deleted = 0,
                empty = 0,
                accountId = accountId.toString(),
            ),
        )

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(3)

        val originalMessageThread = threads.first { it.messageId == originalMessageId }
        assertThat(originalMessageThread.id).isEqualTo(originalMessageThread.root)
        assertThat(originalMessageThread.parent).isNull()

        val destinationMessageThread = threads.first { it.messageId == destinationMessageId }
        assertThat(destinationMessageThread.id).isEqualTo(destinationMessageThread.root)
        assertThat(destinationMessageThread.parent).isNull()

        val childMessageThread = threads.first { it.messageId == childMessageId }
        assertThat(childMessageThread.root).isEqualTo(destinationMessageThread.id)
        assertThat(childMessageThread.parent).isEqualTo(destinationMessageThread.id)
    }

    @Test
    fun `move message not containing a message-id header`() {
        val originalMessageId = sqliteDatabase.createMessage(
            folderId = SOURCE_FOLDER_ID,
            uid = "uid1",
            subject = "Move me",
            messageIdHeader = null,
        )
        sqliteDatabase.createThread(messageId = originalMessageId)
        val originalMessage = sqliteDatabase.readMessages().first()

        val destinationMessageId = moveMessageOperations.moveMessage(
            messageId = originalMessageId,
            destinationFolderId = DESTINATION_FOLDER_ID,
        )

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(2)

        val sourceMessage = messages.first { it.id == originalMessageId }
        assertThat(sourceMessage.folderId).isEqualTo(SOURCE_FOLDER_ID)
        assertThat(sourceMessage.uid).isEqualTo("uid1")
        assertPlaceholderEntry(sourceMessage)

        val destinationMessage = messages.first { it.id == destinationMessageId }
        assertThat(destinationMessage.uid).isNotNull().startsWith(K9.LOCAL_UID_PREFIX)
        assertThat(destinationMessage).isEqualTo(
            originalMessage.copy(
                id = destinationMessageId,
                folderId = DESTINATION_FOLDER_ID,
                uid = destinationMessage.uid,
                deleted = 0,
                empty = 0,
                accountId = accountId.toString(),
            ),
        )

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(2)

        val originalMessageThread = threads.first { it.messageId == originalMessageId }
        assertThat(originalMessageThread.id).isEqualTo(originalMessageThread.root)
        assertThat(originalMessageThread.parent).isNull()

        val destinationMessageThread = threads.first { it.messageId == destinationMessageId }
        assertThat(destinationMessageThread.id).isEqualTo(destinationMessageThread.root)
        assertThat(destinationMessageThread.parent).isNull()
    }

    private fun assertPlaceholderEntry(message: MessageEntry) {
        assertThat(message.deleted).isEqualTo(1)
        assertThat(message.empty).isEqualTo(0)
        assertThat(message.read).isEqualTo(1)
        assertThat(message.date).isNull()
        assertThat(message.flags).isNull()
        assertThat(message.senderList).isNull()
        assertThat(message.toList).isNull()
        assertThat(message.ccList).isNull()
        assertThat(message.bccList).isNull()
        assertThat(message.replyToList).isNull()
        assertThat(message.attachmentCount).isNull()
        assertThat(message.internalDate).isNull()
        assertThat(message.previewType).isEqualTo("none")
        assertThat(message.preview).isNull()
        assertThat(message.mimeType).isNull()
        assertThat(message.normalizedSubjectHash).isNull()
        assertThat(message.flagged).isNull()
        assertThat(message.answered).isNull()
        assertThat(message.forwarded).isNull()
        assertThat(message.messagePartId).isNull()
        assertThat(message.encryptionType).isNull()
    }
}
