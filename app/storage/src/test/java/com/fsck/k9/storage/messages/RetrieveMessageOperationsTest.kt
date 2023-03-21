package com.fsck.k9.storage.messages

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Header
import com.fsck.k9.mail.crlf
import com.fsck.k9.storage.RobolectricTest
import java.util.Date
import org.junit.Test

class RetrieveMessageOperationsTest : RobolectricTest() {
    private val sqliteDatabase = createDatabase()
    private val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
    private val retrieveMessageOperations = RetrieveMessageOperations(lockableDatabase)

    @Test
    fun `get message server id of non-existent message`() {
        val messageServerId = retrieveMessageOperations.getMessageServerId(42)

        assertThat(messageServerId).isNull()
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
                messageId4 to "uid4",
            ),
        )
    }

    @Test
    fun `get all message server ids`() {
        sqliteDatabase.createMessage(folderId = 1, uid = "uid1")
        sqliteDatabase.createMessage(folderId = 1, uid = "K9LOCAL:1")
        sqliteDatabase.createMessage(folderId = 1, uid = "uid3")
        sqliteDatabase.createMessage(folderId = 1, uid = "uid4")

        val messageServerIds = retrieveMessageOperations.getMessageServerIds(folderId = 1)

        assertThat(messageServerIds).isEqualTo(setOf("uid1", "uid3", "uid4"))
    }

    @Test
    fun `check if message is present`() {
        sqliteDatabase.createMessage(folderId = 1, uid = "uid1")

        val result = retrieveMessageOperations.isMessagePresent(folderId = 1, messageServerId = "uid1")

        assertThat(result).isTrue()
    }

    @Test
    fun `check if non-existent message is present`() {
        val result = retrieveMessageOperations.isMessagePresent(folderId = 1, messageServerId = "uid1")

        assertThat(result).isFalse()
    }

    @Test
    fun `get message flags`() {
        sqliteDatabase.createMessage(
            folderId = 1,
            uid = "uid1",
            flags = "DRAFT,RECENT,X_DESTROYED,X_SEND_FAILED,X_SEND_IN_PROGRESS,X_DOWNLOADED_FULL," +
                "X_DOWNLOADED_PARTIAL,X_REMOTE_COPY_STARTED,X_MIGRATED_FROM_V50,X_DRAFT_OPENPGP_INLINE," +
                "X_SUBJECT_DECRYPTED",
            deleted = true,
            read = true,
            flagged = true,
            answered = true,
            forwarded = true,
        )

        val flags = retrieveMessageOperations.getMessageFlags(folderId = 1, messageServerId = "uid1")

        assertThat(flags).isEqualTo(
            setOf(
                Flag.DELETED,
                Flag.SEEN,
                Flag.ANSWERED,
                Flag.FLAGGED,
                Flag.DRAFT,
                Flag.RECENT,
                Flag.FORWARDED,
                Flag.X_DESTROYED,
                Flag.X_SEND_FAILED,
                Flag.X_SEND_IN_PROGRESS,
                Flag.X_DOWNLOADED_FULL,
                Flag.X_DOWNLOADED_PARTIAL,
                Flag.X_REMOTE_COPY_STARTED,
                Flag.X_MIGRATED_FROM_V50,
                Flag.X_DRAFT_OPENPGP_INLINE,
                Flag.X_SUBJECT_DECRYPTED,
            ),
        )
    }

    @Test
    fun `get message flags without any flags set`() {
        sqliteDatabase.createMessage(folderId = 1, uid = "uid1", flags = "")

        val flags = retrieveMessageOperations.getMessageFlags(folderId = 1, messageServerId = "uid1")

        assertThat(flags).isEmpty()
    }

    @Test
    fun `get all message server ids and dates`() {
        sqliteDatabase.createMessage(folderId = 1, uid = "uid1", date = 23)
        sqliteDatabase.createMessage(folderId = 1, uid = "K9LOCAL:1")
        sqliteDatabase.createMessage(folderId = 1, uid = "uid3", date = 42)
        sqliteDatabase.createMessage(folderId = 1, uid = "uid4", deleted = true)

        val result = retrieveMessageOperations.getAllMessagesAndEffectiveDates(folderId = 1)

        assertThat(result).isEqualTo(
            mapOf(
                "uid1" to 23L,
                "uid3" to 42L,
            ),
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
            """.trimIndent().crlf(),
        )
        sqliteDatabase.createMessage(folderId = 1, uid = "uid1", messagePartId = messagePartId)

        val headers = retrieveMessageOperations.getHeaders(folderId = 1, messageServerId = "uid1")

        assertThat(headers).isEqualTo(
            listOf(
                Header("From", "<alice@domain.example>"),
                Header("To", "Bob <bob@domain.example>"),
                Header("Date", "Thu, 01 Apr 2021 01:23:45 +0200"),
                Header("Subject", "Test"),
                Header("Message-Id", "<20210401012345.123456789A@domain.example>"),
            ),
        )
    }

    @Test
    fun `get some headers`() {
        val messagePartId = sqliteDatabase.createMessagePart(
            header = """
                From: <alice@domain.example>
                To: Bob <bob@domain.example>
                Date: Thu, 01 Apr 2021 01:23:45 +0200
                Subject: Test
                Message-Id: <20210401012345.123456789A@domain.example>
            """.trimIndent().crlf(),
        )
        sqliteDatabase.createMessage(folderId = 1, uid = "uid1", messagePartId = messagePartId)

        val headers = retrieveMessageOperations.getHeaders(
            folderId = 1,
            messageServerId = "uid1",
            headerNames = setOf("from", "to", "message-id"),
        )

        assertThat(headers).isEqualTo(
            listOf(
                Header("From", "<alice@domain.example>"),
                Header("To", "Bob <bob@domain.example>"),
                Header("Message-Id", "<20210401012345.123456789A@domain.example>"),
            ),
        )
    }

    @Test
    fun `get oldest message date`() {
        sqliteDatabase.createMessage(folderId = 1, date = 42)
        sqliteDatabase.createMessage(folderId = 1, date = 23)

        val oldestMessageDate = retrieveMessageOperations.getOldestMessageDate(folderId = 1)

        assertThat(oldestMessageDate).isEqualTo(Date(23))
    }

    @Test
    fun `get oldest message date without any messages in the store`() {
        val oldestMessageDate = retrieveMessageOperations.getOldestMessageDate(folderId = 1)

        assertThat(oldestMessageDate).isNull()
    }
}
