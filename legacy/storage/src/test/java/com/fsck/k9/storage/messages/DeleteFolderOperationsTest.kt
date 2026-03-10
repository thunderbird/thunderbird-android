package com.fsck.k9.storage.messages

import android.database.sqlite.SQLiteDatabase
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.extracting
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.fsck.k9.mailstore.StorageFilesProvider
import com.fsck.k9.storage.RobolectricTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class DeleteFolderOperationsTest : RobolectricTest() {
    private val messagePartDirectory = createRandomTempDirectory()
    private lateinit var sqliteDatabase: SQLiteDatabase
    private lateinit var deleteFolderOperations: DeleteFolderOperations

    @Before
    fun setUp() {
        sqliteDatabase = createDatabase()
        val storageFilesProvider = object : StorageFilesProvider {
            override fun getDatabaseFile() = error("Not implemented")
            override fun getAttachmentDirectory() = messagePartDirectory
        }
        val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
        val attachmentFileManager = AttachmentFileManager(storageFilesProvider, mock())
        deleteFolderOperations = DeleteFolderOperations(lockableDatabase, attachmentFileManager)
    }

    @After
    fun tearDown() {
        sqliteDatabase.close()
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
