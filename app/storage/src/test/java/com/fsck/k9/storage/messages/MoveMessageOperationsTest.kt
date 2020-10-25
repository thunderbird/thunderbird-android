package com.fsck.k9.storage.messages

import com.fsck.k9.K9
import com.fsck.k9.storage.RobolectricTest
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.junit.Assert.fail as junitFail

private const val SOURCE_FOLDER_ID = 3L
private const val DESTINATION_FOLDER_ID = 23L
private const val MESSAGE_ID_HEADER = "<00000000-0000-4000-0000-000000000000@domain.example>"

class MoveMessageOperationsTest : RobolectricTest() {
    val sqliteDatabase = createDatabase()
    val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)

    @Test
    fun `move message not part of a thread`() {
        val originalMessageId = sqliteDatabase.createMessage(
            folderId = SOURCE_FOLDER_ID,
            uid = "uid1",
            subject = "Move me",
            messageIdHeader = MESSAGE_ID_HEADER
        )
        val originalMessage = sqliteDatabase.readMessages().first()
        val messageThreader = createMessageThreader(
            ThreadInfo(
                threadId = null,
                messageId = null,
                messageIdHeader = MESSAGE_ID_HEADER,
                rootId = null,
                parentId = null
            )
        )
        val moveMessageOperations = MoveMessageOperations(lockableDatabase, messageThreader)

        val destinationMessageId = moveMessageOperations.moveMessage(
            messageId = originalMessageId,
            destinationFolderId = DESTINATION_FOLDER_ID
        )

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(2)

        val sourceMessage = messages.find { it.id == originalMessageId }
            ?: fail("Original message not found")
        assertThat(sourceMessage.folderId).isEqualTo(SOURCE_FOLDER_ID)
        assertThat(sourceMessage.uid).isEqualTo("uid1")
        assertThat(sourceMessage.messageId).isEqualTo(MESSAGE_ID_HEADER)
        assertPlaceholderEntry(sourceMessage)

        val destinationMessage = messages.find { it.id == destinationMessageId }
            ?: fail("Destination message not found")
        assertThat(destinationMessage.uid).startsWith(K9.LOCAL_UID_PREFIX)
        assertThat(destinationMessage).isEqualTo(
            originalMessage.copy(
                id = destinationMessageId,
                folderId = DESTINATION_FOLDER_ID,
                uid = destinationMessage.uid,
                deleted = 0,
                empty = 0
            )
        )
    }

    @Test
    fun `move message when destination has empty message entry`() {
        val originalMessageId = sqliteDatabase.createMessage(
            folderId = SOURCE_FOLDER_ID,
            uid = "uid1",
            subject = "Move me",
            messageIdHeader = MESSAGE_ID_HEADER,
            read = false
        )
        val originalMessage = sqliteDatabase.readMessages().first()
        val placeholderMessageId = sqliteDatabase.createMessage(
            empty = true,
            folderId = DESTINATION_FOLDER_ID,
            messageIdHeader = MESSAGE_ID_HEADER,
            uid = ""
        )
        val messageThreader = createMessageThreader(
            ThreadInfo(
                threadId = null,
                messageId = placeholderMessageId,
                messageIdHeader = MESSAGE_ID_HEADER,
                rootId = null,
                parentId = null
            )
        )
        val moveMessageOperations = MoveMessageOperations(lockableDatabase, messageThreader)

        val destinationMessageId = moveMessageOperations.moveMessage(
            messageId = originalMessageId,
            destinationFolderId = DESTINATION_FOLDER_ID
        )

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(2)

        val sourceMessage = messages.find { it.id == originalMessageId }
            ?: fail("Original message not found in database")
        assertThat(sourceMessage.folderId).isEqualTo(SOURCE_FOLDER_ID)
        assertThat(sourceMessage.uid).isEqualTo("uid1")
        assertThat(sourceMessage.messageId).isEqualTo(MESSAGE_ID_HEADER)
        assertPlaceholderEntry(sourceMessage)

        val destinationMessage = messages.find { it.id == destinationMessageId }
            ?: fail("Destination message not found in database")
        assertThat(destinationMessage.uid).startsWith(K9.LOCAL_UID_PREFIX)
        assertThat(destinationMessage).isEqualTo(
            originalMessage.copy(
                id = destinationMessageId,
                folderId = DESTINATION_FOLDER_ID,
                uid = destinationMessage.uid,
                deleted = 0,
                empty = 0
            )
        )
    }

    @Test
    fun `move message not containing a message-id header`() {
        val originalMessageId = sqliteDatabase.createMessage(
            folderId = SOURCE_FOLDER_ID,
            uid = "uid1",
            subject = "Move me",
            messageIdHeader = null
        )
        val originalMessage = sqliteDatabase.readMessages().first()
        val messageThreader = createMessageThreader(
            ThreadInfo(
                threadId = null,
                messageId = null,
                messageIdHeader = null,
                rootId = null,
                parentId = null
            )
        )
        val moveMessageOperations = MoveMessageOperations(lockableDatabase, messageThreader)

        val destinationMessageId = moveMessageOperations.moveMessage(
            messageId = originalMessageId,
            destinationFolderId = DESTINATION_FOLDER_ID
        )

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(2)

        val sourceMessage = messages.find { it.id == originalMessageId }
            ?: fail("Original message not found")
        assertThat(sourceMessage.folderId).isEqualTo(SOURCE_FOLDER_ID)
        assertThat(sourceMessage.uid).isEqualTo("uid1")
        assertPlaceholderEntry(sourceMessage)

        val destinationMessage = messages.find { it.id == destinationMessageId }
            ?: fail("Destination message not found")
        assertThat(destinationMessage.uid).startsWith(K9.LOCAL_UID_PREFIX)
        assertThat(destinationMessage).isEqualTo(
            originalMessage.copy(
                id = destinationMessageId,
                folderId = DESTINATION_FOLDER_ID,
                uid = destinationMessage.uid,
                deleted = 0,
                empty = 0
            )
        )
    }

    private fun createMessageThreader(threadInfo: ThreadInfo): ThreadMessageOperations {
        return mock {
            on { createOrUpdateParentThreadEntries(any(), anyLong(), anyLong()) } doReturn threadInfo
        }
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

    private fun fail(message: String): Nothing = junitFail(message) as Nothing
}
