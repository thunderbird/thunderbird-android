package com.fsck.k9.storage.messages

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.extracting
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.fsck.k9.mailstore.StorageManager
import com.fsck.k9.storage.RobolectricTest
import org.junit.After
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

private const val ACCOUNT_UUID = "00000000-0000-4000-0000-000000000000"

class DeleteFolderOperationsTest : RobolectricTest() {
    private val messagePartDirectory = createRandomTempDirectory()
    private val sqliteDatabase = createDatabase()
    private val storageManager = mock<StorageManager> {
        on { getAttachmentDirectory(eq(ACCOUNT_UUID), anyOrNull()) } doReturn messagePartDirectory
    }
    private val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
    private val attachmentFileManager = AttachmentFileManager(storageManager, ACCOUNT_UUID)
    private val deleteFolderOperations = DeleteFolderOperations(lockableDatabase, attachmentFileManager)

    @After
    fun tearDown() {
        messagePartDirectory.deleteRecursively()
    }

    @Test
    fun `delete folder should remove message part files`() {
        createFolderWithMessage("delete", "message1")
        val messagePartId = createFolderWithMessage("retain", "message2")

        deleteFolderOperations.deleteFolders(listOf("delete"))

        val folders = sqliteDatabase.readFolders()
        assertThat(folders).hasSize(1)
        assertThat(folders.first().serverId).isEqualTo("retain")

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(1)
        assertThat(messages.first().uid).isEqualTo("message2")

        val messagePartFiles = messagePartDirectory.listFiles()
        assertThat(messagePartFiles).isNotNull()
            .extracting { it.name }.containsExactly(messagePartId.toString())
    }

    private fun createFolderWithMessage(folderServerId: String, messageServerId: String): Long {
        val folderId = sqliteDatabase.createFolder(serverId = folderServerId)
        val messagePartId = sqliteDatabase.createMessagePart(dataLocation = 2, directory = messagePartDirectory)
        sqliteDatabase.createMessage(folderId = folderId, uid = messageServerId, messagePartId = messagePartId)

        return messagePartId
    }
}
