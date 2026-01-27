package com.fsck.k9.storage.messages

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.fsck.k9.storage.RobolectricTest
import net.thunderbird.core.common.mail.Flag
import org.junit.Test

class FlagMessageOperationsTest : RobolectricTest() {
    private val sqliteDatabase = createDatabase()
    private val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
    private val flagMessageOperations = FlagMessageOperations(lockableDatabase)

    @Test(expected = IllegalArgumentException::class)
    fun `empty messageIds list`() {
        flagMessageOperations.setFlag(emptyList(), Flag.SEEN, true)
    }

    @Test
    fun `mark one message as answered`() {
        val messageId = sqliteDatabase.createMessage(folderId = 1, uid = "uid1", answered = false)
        sqliteDatabase.createMessage(folderId = 1, uid = "uid2", answered = false)
        val messageIds = listOf(messageId)

        flagMessageOperations.setFlag(messageIds, Flag.ANSWERED, true)

        val messages = sqliteDatabase.readMessages()

        val message = messages.find { it.id == messageId } ?: error("Original message not found")
        assertThat(message.answered).isEqualTo(1)

        val otherMessages = messages.filterNot { it.id == messageId }
        assertThat(otherMessages).hasSize(1)
        assertThat(otherMessages.all { it.answered == 0 }).isTrue()
    }

    @Test
    fun `mark multiple messages as read`() {
        val messageId1 = sqliteDatabase.createMessage(folderId = 1, uid = "uid1", read = false)
        val messageId2 = sqliteDatabase.createMessage(folderId = 1, uid = "uid2", read = false)
        val messageId3 = sqliteDatabase.createMessage(folderId = 1, uid = "uid3", read = false)
        sqliteDatabase.createMessage(folderId = 1, uid = "uidx", read = false)
        val messageIds = listOf(messageId1, messageId2, messageId3)

        flagMessageOperations.setFlag(messageIds, Flag.SEEN, true)

        val messages = sqliteDatabase.readMessages()

        val affectedMessages = messages.filter { it.id in messageIds }
        assertThat(affectedMessages).hasSize(3)
        assertThat(affectedMessages.all { it.read == 1 }).isTrue()

        val otherMessages = messages.filterNot { it.id in messageIds }
        assertThat(otherMessages).hasSize(1)
        assertThat(otherMessages.all { it.read == 0 }).isTrue()
    }

    @Test
    fun `mark message as read`() {
        sqliteDatabase.createMessage(folderId = 1, uid = "uid1", read = false)

        flagMessageOperations.setMessageFlag(folderId = 1, messageServerId = "uid1", Flag.SEEN, true)

        val message = sqliteDatabase.readMessages().first()
        assertThat(message.read).isEqualTo(1)
    }

    @Test
    fun `mark message as unread`() {
        sqliteDatabase.createMessage(folderId = 1, uid = "uid1", read = true)

        flagMessageOperations.setMessageFlag(folderId = 1, messageServerId = "uid1", Flag.SEEN, false)

        val message = sqliteDatabase.readMessages().first()
        assertThat(message.read).isEqualTo(0)
    }

    @Test
    fun `mark message as X_DOWNLOADED_FULL`() {
        sqliteDatabase.createMessage(folderId = 1, uid = "uid1", flags = "X_SUBJECT_DECRYPTED")

        flagMessageOperations.setMessageFlag(folderId = 1, messageServerId = "uid1", Flag.X_DOWNLOADED_FULL, true)

        val message = sqliteDatabase.readMessages().first()
        val readFlags = message.flags!!.split(',').toSet()
        assertThat(readFlags).isEqualTo(setOf("X_SUBJECT_DECRYPTED", "X_DOWNLOADED_FULL"))
    }

    @Test
    fun `remove X_DOWNLOADED_FULL flag`() {
        sqliteDatabase.createMessage(folderId = 1, uid = "uid1", flags = "X_DOWNLOADED_FULL")

        flagMessageOperations.setMessageFlag(folderId = 1, messageServerId = "uid1", Flag.X_DOWNLOADED_FULL, false)

        val message = sqliteDatabase.readMessages().first()
        assertThat(message.flags).isEqualTo("")
    }
}
