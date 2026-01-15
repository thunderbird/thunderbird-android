package com.fsck.k9.storage.messages

import android.database.sqlite.SQLiteDatabase
import app.k9mail.legacy.mailstore.MoreMessages
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mailstore.FolderNotFoundException
import com.fsck.k9.mailstore.toDatabaseFolderType
import com.fsck.k9.storage.RobolectricTest
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchAttribute
import org.junit.After
import org.junit.Before
import org.junit.Test

class RetrieveFolderOperationsTest : RobolectricTest() {
    private lateinit var sqliteDatabase: SQLiteDatabase
    private lateinit var retrieveFolderOperations: RetrieveFolderOperations

    @Before
    fun setUp() {
        Log.logger = TestLogger()
        sqliteDatabase = createDatabase()
        val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
        retrieveFolderOperations = RetrieveFolderOperations(lockableDatabase)
    }

    @After
    fun tearDown() {
        sqliteDatabase.close()
    }

    @Test
    fun `get folder`() {
        val folderId = sqliteDatabase.createFolder(
            name = "Folder Name",
            type = "inbox",
            serverId = "uid",
            isLocalOnly = false,
            integrate = true,
            inTopGroup = true,
            visible = true,
            syncEnabled = true,
            notificationsEnabled = true,
            pushEnabled = true,
        )

        val result = retrieveFolderOperations.getFolder(folderId) { folder ->
            assertThat(folder.id).isEqualTo(folderId)
            assertThat(folder.name).isEqualTo("Folder Name")
            assertThat(folder.type).isEqualTo(FolderType.INBOX)
            assertThat(folder.serverId).isEqualTo("uid")
            assertThat(folder.isLocalOnly).isEqualTo(false)
            assertThat(folder.isIntegrate).isEqualTo(true)
            assertThat(folder.isInTopGroup).isEqualTo(true)
            assertThat(folder.isVisible).isEqualTo(true)
            assertThat(folder.isSyncEnabled).isEqualTo(true)
            assertThat(folder.isNotificationsEnabled).isEqualTo(true)
            assertThat(folder.isPushEnabled).isEqualTo(true)
            true
        }

        assertThat(result).isNotNull().isTrue()
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
            visible = true,
            syncEnabled = false,
            notificationsEnabled = true,
            pushEnabled = false,
        )

        val result = retrieveFolderOperations.getFolder(folderId) { folder ->
            assertThat(folder.id).isEqualTo(folderId)
            assertThat(folder.name).isEqualTo(name)
            assertThat(folder.type).isEqualTo(FolderType.OUTBOX)
            assertThat(folder.serverId).isNull()
            assertThat(folder.isLocalOnly).isEqualTo(true)
            assertThat(folder.isIntegrate).isEqualTo(true)
            assertThat(folder.isInTopGroup).isEqualTo(true)
            assertThat(folder.isVisible).isEqualTo(true)
            assertThat(folder.isSyncEnabled).isEqualTo(false)
            assertThat(folder.isNotificationsEnabled).isEqualTo(true)
            assertThat(folder.isPushEnabled).isEqualTo(false)
            true
        }

        assertThat(result).isNotNull().isTrue()
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
            visible = true,
            syncEnabled = true,
            notificationsEnabled = true,
            pushEnabled = false,
        )

        val result = retrieveFolderOperations.getFolder("folder1") { folder ->
            assertThat(folder.id).isEqualTo(folderId)
            assertThat(folder.name).isEqualTo("Folder Name")
            assertThat(folder.type).isEqualTo(FolderType.INBOX)
            assertThat(folder.serverId).isEqualTo("folder1")
            assertThat(folder.isLocalOnly).isEqualTo(false)
            assertThat(folder.isIntegrate).isEqualTo(true)
            assertThat(folder.isInTopGroup).isEqualTo(true)
            assertThat(folder.isVisible).isEqualTo(true)
            assertThat(folder.isSyncEnabled).isEqualTo(true)
            assertThat(folder.isNotificationsEnabled).isEqualTo(true)
            assertThat(folder.isPushEnabled).isEqualTo(false)
            true
        }

        assertThat(result).isNotNull().isTrue()
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
            visible = true,
            syncEnabled = true,
            notificationsEnabled = true,
            pushEnabled = false,
        )

        val result = retrieveFolderOperations.getFolders { folder ->
            assertThat(folder.id).isEqualTo(folderId)
            assertThat(folder.name).isEqualTo("Folder Name")
            assertThat(folder.type).isEqualTo(FolderType.INBOX)
            assertThat(folder.serverId).isEqualTo("uid")
            assertThat(folder.isLocalOnly).isEqualTo(false)
            assertThat(folder.isIntegrate).isEqualTo(true)
            assertThat(folder.isInTopGroup).isEqualTo(true)
            assertThat(folder.isVisible).isEqualTo(true)
            assertThat(folder.isSyncEnabled).isEqualTo(true)
            assertThat(folder.isNotificationsEnabled).isEqualTo(true)
            assertThat(folder.isPushEnabled).isEqualTo(false)
            true
        }

        assertThat(result).isEqualTo(listOf(true))
    }

    @Test
    fun `get folders with excludeLocalOnly should only return remote folders`() {
        val (folderId1, _, folderId3) = listOf(
            sqliteDatabase.createFolder(name = "Folder 1", isLocalOnly = false),
            sqliteDatabase.createFolder(name = "Folder 2", isLocalOnly = true),
            sqliteDatabase.createFolder(name = "Folder 3", isLocalOnly = false),
        )

        val result = retrieveFolderOperations.getFolders(excludeLocalOnly = true) { folder ->
            folder.id to folder.name
        }

        assertThat(result).isEqualTo(
            listOf(
                folderId1 to "Folder 1",
                folderId3 to "Folder 3",
            ),
        )
    }

    @Test
    fun `get folders with empty store should return empty list`() {
        val result = retrieveFolderOperations.getFolders { "failed" }

        assertThat(result).isEmpty()
    }

    @Test
    fun `get visible display folders`() {
        val (folderId1, folderId2, _) = listOf(
            sqliteDatabase.createFolder(name = "Folder 1", visible = true),
            sqliteDatabase.createFolder(name = "Folder 2", visible = false),
            sqliteDatabase.createFolder(name = "Folder 3", visible = false),
        )

        val result = retrieveFolderOperations.getDisplayFolders(
            includeHiddenFolders = false,
            outboxFolderId = folderId2,
        ) { folder ->
            folder.id to folder.name
        }

        assertThat(result).isEqualTo(listOf(folderId1 to "Folder 1"))
    }

    @Test
    fun `get all display folders`() {
        val (folderId1, folderId2, folderId3) = listOf(
            sqliteDatabase.createFolder(name = "Folder 1", visible = true),
            sqliteDatabase.createFolder(name = "Folder 2", visible = true),
            sqliteDatabase.createFolder(name = "Folder 3", visible = false),
        )

        val result = retrieveFolderOperations.getDisplayFolders(
            includeHiddenFolders = true,
            outboxFolderId = folderId1,
        ) { folder ->
            folder.id to folder.name
        }

        assertThat(result).isEqualTo(
            listOf(
                folderId1 to "Folder 1",
                folderId2 to "Folder 2",
                folderId3 to "Folder 3",
            ),
        )
    }

    @Test
    fun `get display folders with message count`() {
        val folderIds = listOf(
            sqliteDatabase.createFolder(name = "Folder 1"),
            sqliteDatabase.createFolder(name = "Folder 2"),
            sqliteDatabase.createFolder(name = "Folder 3"),
            sqliteDatabase.createFolder(name = "Folder 4"),
        )
        val folderId1 = folderIds[0]
        val folderId2 = folderIds[1]
        val folderId3 = folderIds[2]
        val folderId4 = folderIds[3]
        sqliteDatabase.createMessage(uid = "msg1", folderId = folderId1, read = true)
        sqliteDatabase.createMessage(uid = "msg2", folderId = folderId2, read = true)
        sqliteDatabase.createMessage(uid = "msg3", folderId = folderId3, read = true)
        sqliteDatabase.createMessage(uid = "msg4", folderId = folderId3, read = false)
        sqliteDatabase.createMessage(uid = "msg5", folderId = folderId3, read = false)

        val result = retrieveFolderOperations.getDisplayFolders(
            includeHiddenFolders = true,
            outboxFolderId = folderId2,
        ) { folder ->
            Triple(folder.id, folder.name, folder.unreadMessageCount)
        }

        assertThat(result).hasSize(4)
        assertThat(result.toSet()).isEqualTo(
            setOf(
                Triple(folderId1, "Folder 1", 0),
                Triple(folderId2, "Folder 2", 1),
                Triple(folderId3, "Folder 3", 2),
                Triple(folderId4, "Folder 4", 0),
            ),
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

    @Test
    fun `get unread message count from empty folder`() {
        val folderId = sqliteDatabase.createFolder()

        val result = retrieveFolderOperations.getUnreadMessageCount(folderId)

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `get unread message count from non-existent folder`() {
        val result = retrieveFolderOperations.getUnreadMessageCount(23)

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `get unread message count from non-empty folder`() {
        val folderId = sqliteDatabase.createFolder()
        sqliteDatabase.createMessage(folderId = folderId, read = false)
        sqliteDatabase.createMessage(folderId = folderId, read = false)
        sqliteDatabase.createMessage(folderId = folderId, read = true)

        val result = retrieveFolderOperations.getUnreadMessageCount(folderId)

        assertThat(result).isEqualTo(2)
    }

    @Test
    fun `get unread message count with condition from empty folder`() {
        sqliteDatabase.createFolder(integrate = true)

        val result = retrieveFolderOperations.getUnreadMessageCount(unifiedInboxConditions)

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `get unread message count with condition from non-existent folder`() {
        val result = retrieveFolderOperations.getUnreadMessageCount(unifiedInboxConditions)

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `get unread message count with condition from non-empty folder`() {
        val folderId1 = sqliteDatabase.createFolder(integrate = true)
        sqliteDatabase.createMessage(folderId = folderId1, read = false)
        sqliteDatabase.createMessage(folderId = folderId1, read = false)
        sqliteDatabase.createMessage(folderId = folderId1, read = true)
        val folderId2 = sqliteDatabase.createFolder(integrate = true)
        sqliteDatabase.createMessage(folderId = folderId2, read = false)
        sqliteDatabase.createMessage(folderId = folderId2, read = true)
        val folderId3 = sqliteDatabase.createFolder(integrate = false)
        sqliteDatabase.createMessage(folderId = folderId3, read = false)

        val result = retrieveFolderOperations.getUnreadMessageCount(unifiedInboxConditions)

        assertThat(result).isEqualTo(3)
    }

    @Test
    fun `get unread message count without condition`() {
        val folderId1 = sqliteDatabase.createFolder(integrate = true)
        sqliteDatabase.createMessage(folderId = folderId1, read = false)
        val folderId2 = sqliteDatabase.createFolder(integrate = false)
        sqliteDatabase.createMessage(folderId = folderId2, read = false)
        sqliteDatabase.createMessage(folderId = folderId2, read = true)

        val result = retrieveFolderOperations.getUnreadMessageCount(conditions = null)

        assertThat(result).isEqualTo(2)
    }

    @Test
    fun `get starred message count with condition from empty folder`() {
        sqliteDatabase.createFolder(integrate = true)

        val result = retrieveFolderOperations.getStarredMessageCount(unifiedInboxConditions)

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `get starred message count with condition from non-existent folder`() {
        val result = retrieveFolderOperations.getStarredMessageCount(unifiedInboxConditions)

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `get starred message count with condition from non-empty folder`() {
        val folderId1 = sqliteDatabase.createFolder(integrate = true)
        sqliteDatabase.createMessage(folderId = folderId1, flagged = false)
        sqliteDatabase.createMessage(folderId = folderId1, flagged = true)
        val folderId2 = sqliteDatabase.createFolder(integrate = true)
        sqliteDatabase.createMessage(folderId = folderId2, flagged = true)
        sqliteDatabase.createMessage(folderId = folderId2, flagged = true)
        sqliteDatabase.createMessage(folderId = folderId2, flagged = false)
        val folderId3 = sqliteDatabase.createFolder(integrate = false)
        sqliteDatabase.createMessage(folderId = folderId3, flagged = true)

        val result = retrieveFolderOperations.getStarredMessageCount(unifiedInboxConditions)

        assertThat(result).isEqualTo(3)
    }

    @Test
    fun `get starred message count without condition`() {
        val folderId1 = sqliteDatabase.createFolder(integrate = true)
        sqliteDatabase.createMessage(folderId = folderId1, flagged = true)
        val folderId2 = sqliteDatabase.createFolder(integrate = false)
        sqliteDatabase.createMessage(folderId = folderId2, flagged = true)
        sqliteDatabase.createMessage(folderId = folderId2, flagged = false)

        val result = retrieveFolderOperations.getStarredMessageCount(conditions = null)

        assertThat(result).isEqualTo(2)
    }

    @Test
    fun `get 'more messages' value from non-existent folder`() {
        assertFailure {
            retrieveFolderOperations.hasMoreMessages(23)
        }.isInstanceOf<FolderNotFoundException>()
            .transform { it.folderId }.isEqualTo(23)
    }

    @Test
    fun `get 'more messages' value from folder with value 'unknown'`() {
        val folderId = sqliteDatabase.createFolder(moreMessages = "unknown")

        val result = retrieveFolderOperations.hasMoreMessages(folderId)

        assertThat(result).isEqualTo(MoreMessages.UNKNOWN)
    }

    @Test
    fun `get 'more messages' value from folder with value 'false'`() {
        val folderId = sqliteDatabase.createFolder(moreMessages = "false")

        val result = retrieveFolderOperations.hasMoreMessages(folderId)

        assertThat(result).isEqualTo(MoreMessages.FALSE)
    }

    @Test
    fun `get 'more messages' value from folder with value 'true'`() {
        val folderId = sqliteDatabase.createFolder(moreMessages = "true")

        val result = retrieveFolderOperations.hasMoreMessages(folderId)

        assertThat(result).isEqualTo(MoreMessages.TRUE)
    }

    private val unifiedInboxConditions = LocalMessageSearch().apply {
        and(
            MessageSearchField.INTEGRATE,
            "1",
            SearchAttribute.EQUALS,
        )
    }.conditions
}
