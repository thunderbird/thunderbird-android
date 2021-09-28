package com.fsck.k9.storage.messages

import com.fsck.k9.Account.FolderMode
import com.fsck.k9.mail.FolderClass
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mailstore.toDatabaseFolderType
import com.fsck.k9.storage.RobolectricTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RetrieveFolderOperationsTest : RobolectricTest() {
    private val sqliteDatabase = createDatabase()
    private val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
    private val retrieveFolderOperations = RetrieveFolderOperations(lockableDatabase)

    @Test
    fun `get folder`() {
        val folderId = sqliteDatabase.createFolder(
            name = "Folder Name",
            type = "inbox",
            serverId = "uid",
            isLocalOnly = false,
            integrate = true,
            inTopGroup = true,
            displayClass = "FIRST_CLASS",
            syncClass = "FIRST_CLASS",
            notifyClass = "NO_CLASS",
            pushClass = "NO_CLASS"
        )

        val result = retrieveFolderOperations.getFolder(folderId) { folder ->
            assertThat(folder.id).isEqualTo(folderId)
            assertThat(folder.name).isEqualTo("Folder Name")
            assertThat(folder.type).isEqualTo(FolderType.INBOX)
            assertThat(folder.serverId).isEqualTo("uid")
            assertThat(folder.isLocalOnly).isEqualTo(false)
            assertThat(folder.isIntegrate).isEqualTo(true)
            assertThat(folder.isInTopGroup).isEqualTo(true)
            assertThat(folder.displayClass).isEqualTo(FolderClass.FIRST_CLASS)
            assertThat(folder.syncClass).isEqualTo(FolderClass.FIRST_CLASS)
            assertThat(folder.notifyClass).isEqualTo(FolderClass.NO_CLASS)
            assertThat(folder.pushClass).isEqualTo(FolderClass.NO_CLASS)
            true
        }

        assertThat(result).isTrue()
    }

    @Test
    fun `get local folder`() {
        val name = "Local outbox"
        val folderId = sqliteDatabase.createFolder(
            name = name,
            type = FolderType.OUTBOX.toDatabaseFolderType(),
            serverId = null,
            isLocalOnly = true,
            integrate = true,
            inTopGroup = true,
            displayClass = FolderClass.FIRST_CLASS.name,
            syncClass = null,
            notifyClass = null,
            pushClass = null
        )

        val result = retrieveFolderOperations.getFolder(folderId) { folder ->
            assertThat(folder.id).isEqualTo(folderId)
            assertThat(folder.name).isEqualTo(name)
            assertThat(folder.type).isEqualTo(FolderType.OUTBOX)
            assertThat(folder.serverId).isNull()
            assertThat(folder.isLocalOnly).isEqualTo(true)
            assertThat(folder.isIntegrate).isEqualTo(true)
            assertThat(folder.isInTopGroup).isEqualTo(true)
            assertThat(folder.displayClass).isEqualTo(FolderClass.FIRST_CLASS)
            assertThat(folder.syncClass).isEqualTo(FolderClass.INHERITED)
            assertThat(folder.notifyClass).isEqualTo(FolderClass.INHERITED)
            assertThat(folder.pushClass).isEqualTo(FolderClass.SECOND_CLASS)
            true
        }

        assertThat(result).isTrue()
    }

    @Test
    fun `get non-existent folder should return null`() {
        val result = retrieveFolderOperations.getFolder(1) { "failed" }

        assertThat(result).isNull()
    }

    @Test
    fun `get folder by server ID`() {
        val folderId = sqliteDatabase.createFolder(
            name = "Folder Name",
            type = "inbox",
            serverId = "folder1",
            isLocalOnly = false,
            integrate = true,
            inTopGroup = true,
            displayClass = "FIRST_CLASS",
            syncClass = "FIRST_CLASS",
            notifyClass = "NO_CLASS",
            pushClass = "NO_CLASS"
        )

        val result = retrieveFolderOperations.getFolder("folder1") { folder ->
            assertThat(folder.id).isEqualTo(folderId)
            assertThat(folder.name).isEqualTo("Folder Name")
            assertThat(folder.type).isEqualTo(FolderType.INBOX)
            assertThat(folder.serverId).isEqualTo("folder1")
            assertThat(folder.isLocalOnly).isEqualTo(false)
            assertThat(folder.isIntegrate).isEqualTo(true)
            assertThat(folder.isInTopGroup).isEqualTo(true)
            assertThat(folder.displayClass).isEqualTo(FolderClass.FIRST_CLASS)
            assertThat(folder.syncClass).isEqualTo(FolderClass.FIRST_CLASS)
            assertThat(folder.notifyClass).isEqualTo(FolderClass.NO_CLASS)
            assertThat(folder.pushClass).isEqualTo(FolderClass.NO_CLASS)
            true
        }

        assertThat(result).isTrue()
    }

    @Test
    fun `get non-existent folder by server ID should return null`() {
        val result = retrieveFolderOperations.getFolder("folder_id") { "failed" }

        assertThat(result).isNull()
    }

    @Test
    fun `get folders should return all fields`() {
        val folderId = sqliteDatabase.createFolder(
            name = "Folder Name",
            type = "inbox",
            serverId = "uid",
            isLocalOnly = false,
            integrate = true,
            inTopGroup = true,
            displayClass = "FIRST_CLASS",
            syncClass = "FIRST_CLASS",
            notifyClass = "NO_CLASS",
            pushClass = "NO_CLASS"
        )

        val result = retrieveFolderOperations.getFolders { folder ->
            assertThat(folder.id).isEqualTo(folderId)
            assertThat(folder.name).isEqualTo("Folder Name")
            assertThat(folder.type).isEqualTo(FolderType.INBOX)
            assertThat(folder.serverId).isEqualTo("uid")
            assertThat(folder.isLocalOnly).isEqualTo(false)
            assertThat(folder.isIntegrate).isEqualTo(true)
            assertThat(folder.isInTopGroup).isEqualTo(true)
            assertThat(folder.displayClass).isEqualTo(FolderClass.FIRST_CLASS)
            assertThat(folder.syncClass).isEqualTo(FolderClass.FIRST_CLASS)
            assertThat(folder.notifyClass).isEqualTo(FolderClass.NO_CLASS)
            assertThat(folder.pushClass).isEqualTo(FolderClass.NO_CLASS)
            true
        }

        assertThat(result).isEqualTo(listOf(true))
    }

    @Test
    fun `get folders with excludeLocalOnly should only return remote folders`() {
        val (folderId1, _, folderId3) = listOf(
            sqliteDatabase.createFolder(name = "Folder 1", isLocalOnly = false),
            sqliteDatabase.createFolder(name = "Folder 2", isLocalOnly = true),
            sqliteDatabase.createFolder(name = "Folder 3", isLocalOnly = false)
        )

        val result = retrieveFolderOperations.getFolders(excludeLocalOnly = true) { folder ->
            folder.id to folder.name
        }

        assertThat(result).isEqualTo(
            listOf(
                folderId1 to "Folder 1",
                folderId3 to "Folder 3"
            )
        )
    }

    @Test
    fun `get folders with empty store should return empty list`() {
        val result = retrieveFolderOperations.getFolders { "failed" }

        assertThat(result).isEmpty()
    }

    @Test
    fun `get first class display folders`() {
        val (folderId1, folderId2, _) = listOf(
            sqliteDatabase.createFolder(name = "Folder 1", displayClass = "FIRST_CLASS"),
            sqliteDatabase.createFolder(name = "Folder 2", displayClass = "SECOND_CLASS"),
            sqliteDatabase.createFolder(name = "Folder 3", displayClass = "NO_CLASS"),
        )

        val result = retrieveFolderOperations.getDisplayFolders(
            displayMode = FolderMode.FIRST_CLASS,
            outboxFolderId = folderId2
        ) { folder ->
            folder.id to folder.name
        }

        assertThat(result).isEqualTo(listOf(folderId1 to "Folder 1"))
    }

    @Test
    fun `get everything but second class display folders`() {
        val (folderId1, _, folderId3) = listOf(
            sqliteDatabase.createFolder(name = "Folder 1", displayClass = "FIRST_CLASS"),
            sqliteDatabase.createFolder(name = "Folder 2", displayClass = "SECOND_CLASS"),
            sqliteDatabase.createFolder(name = "Folder 3", displayClass = "NO_CLASS"),
        )

        val result = retrieveFolderOperations.getDisplayFolders(
            displayMode = FolderMode.NOT_SECOND_CLASS,
            outboxFolderId = folderId1
        ) { folder ->
            folder.id to folder.name
        }

        assertThat(result).isEqualTo(
            listOf(
                folderId1 to "Folder 1",
                folderId3 to "Folder 3"
            )
        )
    }

    @Test
    fun `get first and second class display folders`() {
        val (folderId1, folderId2, _) = listOf(
            sqliteDatabase.createFolder(name = "Folder 1", displayClass = "FIRST_CLASS"),
            sqliteDatabase.createFolder(name = "Folder 2", displayClass = "SECOND_CLASS"),
            sqliteDatabase.createFolder(name = "Folder 3", displayClass = "NO_CLASS"),
        )

        val result = retrieveFolderOperations.getDisplayFolders(
            displayMode = FolderMode.FIRST_AND_SECOND_CLASS,
            outboxFolderId = folderId1
        ) { folder ->
            folder.id to folder.name
        }

        assertThat(result).isEqualTo(
            listOf(
                folderId1 to "Folder 1",
                folderId2 to "Folder 2"
            )
        )
    }

    @Test
    fun `get display folders with message count`() {
        val (folderId1, folderId2, folderId3, folderId4) = listOf(
            sqliteDatabase.createFolder(name = "Folder 1", displayClass = "FIRST_CLASS"),
            sqliteDatabase.createFolder(name = "Folder 2", displayClass = "SECOND_CLASS"),
            sqliteDatabase.createFolder(name = "Folder 3", displayClass = "FIRST_CLASS"),
            sqliteDatabase.createFolder(name = "Folder 4", displayClass = "NO_CLASS")
        )
        sqliteDatabase.createMessage(uid = "msg1", folderId = folderId1, read = true)
        sqliteDatabase.createMessage(uid = "msg2", folderId = folderId2, read = true)
        sqliteDatabase.createMessage(uid = "msg3", folderId = folderId3, read = true)
        sqliteDatabase.createMessage(uid = "msg4", folderId = folderId3, read = false)
        sqliteDatabase.createMessage(uid = "msg5", folderId = folderId3, read = false)

        val result = retrieveFolderOperations.getDisplayFolders(
            displayMode = FolderMode.ALL,
            outboxFolderId = folderId2
        ) { folder ->
            Triple(folder.id, folder.name, folder.unreadMessageCount)
        }

        assertThat(result).hasSize(4)
        assertThat(result.toSet()).isEqualTo(
            setOf(
                Triple(folderId1, "Folder 1", 0),
                Triple(folderId2, "Folder 2", 1),
                Triple(folderId3, "Folder 3", 2),
                Triple(folderId4, "Folder 4", 0)
            )
        )
    }

    @Test
    fun `get folder id`() {
        val (_, folderId2) = listOf(
            sqliteDatabase.createFolder(serverId = "folder1"),
            sqliteDatabase.createFolder(serverId = "folder2"),
        )

        val result = retrieveFolderOperations.getFolderId(folderServerId = "folder2")

        assertThat(result).isEqualTo(folderId2)
    }

    @Test
    fun `get folder id should return null if no folder was found`() {
        val result = retrieveFolderOperations.getFolderId(folderServerId = "folderId")

        assertThat(result).isNull()
    }

    @Test
    fun `get folder server id`() {
        val (_, folderId2) = listOf(
            sqliteDatabase.createFolder(serverId = "folder1"),
            sqliteDatabase.createFolder(serverId = "folder2"),
        )

        val result = retrieveFolderOperations.getFolderServerId(folderId2)

        assertThat(result).isEqualTo("folder2")
    }

    @Test
    fun `get folder server id should return null if no folder was found`() {
        val result = retrieveFolderOperations.getFolderServerId(folderId = 1)

        assertThat(result).isNull()
    }

    @Test
    fun `get message count from empty folder`() {
        val folderId = sqliteDatabase.createFolder()

        val result = retrieveFolderOperations.getMessageCount(folderId)

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `get message count from non-existent folder`() {
        val result = retrieveFolderOperations.getMessageCount(23)

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `get message count from non-empty folder`() {
        val folderId = sqliteDatabase.createFolder()
        sqliteDatabase.createMessage(folderId = folderId)
        sqliteDatabase.createMessage(folderId = folderId)

        val result = retrieveFolderOperations.getMessageCount(folderId)

        assertThat(result).isEqualTo(2)
    }
}
