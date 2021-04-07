package com.fsck.k9.storage.messages

import com.fsck.k9.mailstore.StorageManager
import com.fsck.k9.storage.RobolectricTest
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Before
import org.junit.Test

private const val ACCOUNT_UUID = "00000000-0000-4000-0000-000000000000"

class DeleteFolderOperationsTest : RobolectricTest() {
    private val sqliteDatabase = createDatabase()
    private val storageManager = mock<StorageManager> {
        on { getAttachmentDirectory(eq(ACCOUNT_UUID), anyOrNull()) } doAnswer { messagePartDirectory }
    }
    private val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
    private val deleteFolderOperations = DeleteFolderOperations(storageManager, lockableDatabase, ACCOUNT_UUID)

    private lateinit var messagePartDirectory: File

    @Before
    fun setUp() {
        messagePartDirectory = createRandomTempDirectory()
    }

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
        assertThat(messagePartFiles).hasLength(1)
        assertThat(messagePartFiles!!.first().name).isEqualTo(messagePartId.toString())
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun createRandomTempDirectory(): File {
        val tempDirectory = File(System.getProperty("java.io.tmpdir", "."))
        return File(tempDirectory, UUID.randomUUID().toString()).also { it.mkdir() }
    }

    private fun createFolderWithMessage(folderServerId: String, messageServerId: String): Long {
        val folderId = sqliteDatabase.createFolder(serverId = folderServerId)
        val messagePartId = sqliteDatabase.createMessagePart(dataLocation = 2, directory = messagePartDirectory)
        sqliteDatabase.createMessage(folderId = folderId, uid = messageServerId, messagePartId = messagePartId)

        return messagePartId
    }
}
