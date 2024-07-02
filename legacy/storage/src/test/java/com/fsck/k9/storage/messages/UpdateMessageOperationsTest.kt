package com.fsck.k9.storage.messages

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.fsck.k9.storage.RobolectricTest
import org.junit.Test

class UpdateMessageOperationsTest : RobolectricTest() {
    private val sqliteDatabase = createDatabase()
    private val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
    private val updateMessageOperations = UpdateMessageOperations(lockableDatabase)

    @Test
    fun `mark message as new`() {
        sqliteDatabase.createMessage(folderId = 1, uid = "uid1", newMessage = false)

        updateMessageOperations.setNewMessageState(folderId = 1, messageServerId = "uid1", newMessage = true)

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(1)

        val message = messages.first()
        assertThat(message.newMessage).isEqualTo(1)
    }

    @Test
    fun `mark message as not new`() {
        sqliteDatabase.createMessage(folderId = 1, uid = "uid1", newMessage = true)

        updateMessageOperations.setNewMessageState(folderId = 1, messageServerId = "uid1", newMessage = false)

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(1)

        val message = messages.first()
        assertThat(message.newMessage).isEqualTo(0)
    }

    @Test
    fun `clear new message state`() {
        sqliteDatabase.createMessage(folderId = 1, uid = "uid1", newMessage = true)
        sqliteDatabase.createMessage(folderId = 1, uid = "uid1", newMessage = false)

        updateMessageOperations.clearNewMessageState()

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(2)

        for (message in messages) {
            assertThat(message.newMessage).isEqualTo(0)
        }
    }
}
