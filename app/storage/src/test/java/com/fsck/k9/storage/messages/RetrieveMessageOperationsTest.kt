package com.fsck.k9.storage.messages

import com.fsck.k9.mail.Header
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.crlf
import com.fsck.k9.storage.RobolectricTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RetrieveMessageOperationsTest : RobolectricTest() {
    private val sqliteDatabase = createDatabase()
    private val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
    private val retrieveMessageOperations = RetrieveMessageOperations(lockableDatabase)

    @Test(expected = MessagingException::class)
    fun `get message server id of non-existent message`() {
        retrieveMessageOperations.getMessageServerId(42)
    }

    @Test
    fun `get message server id`() {
        sqliteDatabase.createMessage(folderId = 1, uid = "uid1")
        val messageId = sqliteDatabase.createMessage(folderId = 1, uid = "uid2")

        val messageServerId = retrieveMessageOperations.getMessageServerId(messageId)

        assertThat(messageServerId).isEqualTo("uid2")
    }

    @Test
    fun `get message server ids`() {
        val messageId1 = sqliteDatabase.createMessage(folderId = 1, uid = "uid1")
        val messageId2 = sqliteDatabase.createMessage(folderId = 1, uid = "uid2")
        val messageId3 = sqliteDatabase.createMessage(folderId = 1, uid = "uid3")
        val messageId4 = sqliteDatabase.createMessage(folderId = 1, uid = "uid4")
        sqliteDatabase.createMessage(folderId = 1, uid = "uid5")
        val messageIds = listOf(messageId1, messageId2, messageId3, messageId4)

        val databaseIdToServerIdMapping = retrieveMessageOperations.getMessageServerIds(messageIds)

        assertThat(databaseIdToServerIdMapping).isEqualTo(
            mapOf(
                messageId1 to "uid1",
                messageId2 to "uid2",
                messageId3 to "uid3",
                messageId4 to "uid4"
            )
        )
    }

    @Test
    fun `get headers`() {
        val messagePartId = sqliteDatabase.createMessagePart(
            header = """
                From: <alice@domain.example>
                To: Bob <bob@domain.example>
                Date: Thu, 01 Apr 2021 01:23:45 +0200
                Subject: Test
                Message-Id: <20210401012345.123456789A@domain.example>
            """.trimIndent().crlf()
        )
        sqliteDatabase.createMessage(folderId = 1, uid = "uid1", messagePartId = messagePartId)

        val headers = retrieveMessageOperations.getHeaders(folderId = 1, messageServerId = "uid1")

        assertThat(headers).isEqualTo(
            listOf(
                Header("From", "<alice@domain.example>"),
                Header("To", "Bob <bob@domain.example>"),
                Header("Date", "Thu, 01 Apr 2021 01:23:45 +0200"),
                Header("Subject", "Test"),
                Header("Message-Id", "<20210401012345.123456789A@domain.example>")
            )
        )
    }

    @Test
    fun `get highest message uid`() {
        val folderId = sqliteDatabase.createFolder()
        sqliteDatabase.createMessage(uid = "42", folderId = folderId)
        sqliteDatabase.createMessage(uid = "23", folderId = folderId)
        sqliteDatabase.createMessage(uid = "27", folderId = folderId)

        val highestUid = retrieveMessageOperations.getLastUid(folderId)

        assertThat(highestUid).isEqualTo(42)
    }

    @Test
    fun `get highest message uid should return null if there are no messages`() {
        val folderId = sqliteDatabase.createFolder()

        val highestUid = retrieveMessageOperations.getLastUid(folderId)

        assertThat(highestUid).isNull()
    }
}
