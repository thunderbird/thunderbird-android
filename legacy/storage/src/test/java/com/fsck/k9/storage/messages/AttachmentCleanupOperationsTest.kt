package com.fsck.k9.storage.messages

import android.database.sqlite.SQLiteDatabase
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.fsck.k9.mailstore.StorageFilesProvider
import com.fsck.k9.storage.RobolectricTest
import net.thunderbird.core.common.mail.Flag
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class AttachmentCleanupOperationsTest : RobolectricTest() {
    private val messagePartDirectory = createRandomTempDirectory()
    private lateinit var sqliteDatabase: SQLiteDatabase
    private lateinit var testSubject: AttachmentCleanupOperations

    @Before
    fun setUp() {
        sqliteDatabase = createDatabase()
        val storageFilesProvider = object : StorageFilesProvider {
            override fun getDatabaseFile() = error("Not implemented")
            override fun getAttachmentDirectory() = messagePartDirectory
        }
        val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
        val attachmentFileManager = AttachmentFileManager(storageFilesProvider, mock())
        testSubject = AttachmentCleanupOperations(lockableDatabase, attachmentFileManager)
    }

    @After
    fun tearDown() {
        sqliteDatabase.close()
        messagePartDirectory.deleteRecursively()
    }

    @Test
    fun `removeOldDownloadedAttachments should remove explicit attachment data from old messages`() {
        val folderId = sqliteDatabase.createFolder(isLocalOnly = false)
        val rootPartId = sqliteDatabase.createMessagePart(dataLocation = DataLocation.CHILD_PART_CONTAINS_DATA)
        val attachmentPartId = sqliteDatabase.createMessagePart(
            root = rootPartId,
            parent = rootPartId,
            mimeType = "application/pdf",
            displayName = "document.pdf",
            header = "Content-Type: application/pdf\r\nContent-Disposition: attachment; filename=\"document.pdf\"\r\n",
            dataLocation = DataLocation.IN_DATABASE,
            data = "content".toByteArray(),
            serverExtra = "2",
        )
        sqliteDatabase.createMessage(
            folderId = folderId,
            uid = "uid1",
            date = 100,
            internalDate = 100,
            flags = Flag.X_DOWNLOADED_FULL.name,
            messagePartId = rootPartId,
        )

        val changedParts = testSubject.removeOldDownloadedAttachments(cutoffTime = 200)

        val attachmentPart = sqliteDatabase.readMessageParts().single { it.id == attachmentPartId }
        val message = sqliteDatabase.readMessages().single()
        assertThat(changedParts).isEqualTo(1)
        assertThat(attachmentPart.dataLocation).isEqualTo(DataLocation.MISSING)
        assertThat(attachmentPart.data).isNull()
        assertThat(attachmentPart.displayName).isEqualTo("document.pdf")
        assertThat(attachmentPart.serverExtra).isEqualTo("2")
        assertThat(message.flags.orEmpty()).doesNotContain(Flag.X_DOWNLOADED_FULL.name)
        assertThat(message.flags.orEmpty()).contains(Flag.X_DOWNLOADED_PARTIAL.name)
    }

    @Test
    fun `removeOldDownloadedAttachments should delete disk backed attachment files`() {
        val folderId = sqliteDatabase.createFolder(isLocalOnly = false)
        val rootPartId = sqliteDatabase.createMessagePart(dataLocation = DataLocation.CHILD_PART_CONTAINS_DATA)
        val attachmentPartId = sqliteDatabase.createMessagePart(
            root = rootPartId,
            parent = rootPartId,
            header = "Content-Disposition: attachment\r\n",
            dataLocation = DataLocation.ON_DISK,
        )
        val attachmentFile = messagePartDirectory.resolve(attachmentPartId.toString()).apply {
            writeText("content")
        }
        sqliteDatabase.createMessage(
            folderId = folderId,
            uid = "uid1",
            date = 100,
            internalDate = 100,
            flags = Flag.X_DOWNLOADED_FULL.name,
            messagePartId = rootPartId,
        )

        val changedParts = testSubject.removeOldDownloadedAttachments(cutoffTime = 200)

        assertThat(changedParts).isEqualTo(1)
        assertThat(attachmentFile.exists()).isFalse()
    }

    @Test
    fun `removeOldDownloadedAttachments should keep inline and recent parts`() {
        val folderId = sqliteDatabase.createFolder(isLocalOnly = false)
        val oldRootPartId = sqliteDatabase.createMessagePart(dataLocation = DataLocation.CHILD_PART_CONTAINS_DATA)
        val inlinePartId = sqliteDatabase.createMessagePart(
            root = oldRootPartId,
            parent = oldRootPartId,
            header = "Content-Disposition: inline; filename=\"attachment.png\"\r\n",
            dataLocation = DataLocation.IN_DATABASE,
            data = "inline".toByteArray(),
        )
        sqliteDatabase.createMessage(
            folderId = folderId,
            uid = "uid1",
            date = 100,
            internalDate = 100,
            flags = Flag.X_DOWNLOADED_FULL.name,
            messagePartId = oldRootPartId,
        )

        val recentRootPartId = sqliteDatabase.createMessagePart(dataLocation = DataLocation.CHILD_PART_CONTAINS_DATA)
        val recentAttachmentPartId = sqliteDatabase.createMessagePart(
            root = recentRootPartId,
            parent = recentRootPartId,
            header = "Content-Disposition: attachment\r\n",
            dataLocation = DataLocation.IN_DATABASE,
            data = "recent".toByteArray(),
        )
        sqliteDatabase.createMessage(
            folderId = folderId,
            uid = "uid2",
            date = 300,
            internalDate = 300,
            flags = Flag.X_DOWNLOADED_FULL.name,
            messagePartId = recentRootPartId,
        )

        val changedParts = testSubject.removeOldDownloadedAttachments(cutoffTime = 200)

        val messageParts = sqliteDatabase.readMessageParts().associateBy { it.id }
        assertThat(changedParts).isEqualTo(0)
        assertThat(messageParts[inlinePartId]?.dataLocation).isEqualTo(DataLocation.IN_DATABASE)
        assertThat(messageParts[recentAttachmentPartId]?.dataLocation).isEqualTo(DataLocation.IN_DATABASE)
        assertThat(messageParts[inlinePartId]?.data?.isNotEmpty() == true).isTrue()
        assertThat(messageParts[recentAttachmentPartId]?.data?.isNotEmpty() == true).isTrue()
    }

    @Test
    fun `removeOldDownloadedAttachments should remove attachments with folded disposition headers`() {
        val folderId = sqliteDatabase.createFolder(isLocalOnly = false)
        val rootPartId = sqliteDatabase.createMessagePart(dataLocation = DataLocation.CHILD_PART_CONTAINS_DATA)
        val attachmentPartId = sqliteDatabase.createMessagePart(
            root = rootPartId,
            parent = rootPartId,
            header = "Content-Disposition :\r\n attachment; filename=\"document.pdf\"\r\n",
            dataLocation = DataLocation.IN_DATABASE,
            data = "content".toByteArray(),
            serverExtra = "2",
        )
        sqliteDatabase.createMessage(
            folderId = folderId,
            uid = "uid1",
            date = 100,
            internalDate = 100,
            flags = Flag.X_DOWNLOADED_FULL.name,
            messagePartId = rootPartId,
        )

        val changedParts = testSubject.removeOldDownloadedAttachments(cutoffTime = 200)

        val attachmentPart = sqliteDatabase.readMessageParts().single { it.id == attachmentPartId }
        assertThat(changedParts).isEqualTo(1)
        assertThat(attachmentPart.dataLocation).isEqualTo(DataLocation.MISSING)
        assertThat(attachmentPart.data).isNull()
    }
}
