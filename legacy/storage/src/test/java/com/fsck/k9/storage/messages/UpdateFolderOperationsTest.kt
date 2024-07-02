package com.fsck.k9.storage.messages

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.FolderClass
import com.fsck.k9.mailstore.Folder
import com.fsck.k9.mailstore.FolderDetails
import com.fsck.k9.mailstore.FolderType
import com.fsck.k9.mailstore.MoreMessages
import com.fsck.k9.storage.RobolectricTest
import org.junit.Test
import com.fsck.k9.mail.FolderType as RemoteFolderType

class UpdateFolderOperationsTest : RobolectricTest() {
    private val sqliteDatabase = createDatabase()
    private val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
    private val updateFolderOperations = UpdateFolderOperations(lockableDatabase)

    @Test
    fun `change folder`() {
        sqliteDatabase.createFolder(serverId = "folder1", name = "Old", type = "REGULAR")

        updateFolderOperations.changeFolder("folder1", "New", RemoteFolderType.TRASH)

        val folder = sqliteDatabase.readFolders().first()
        assertThat(folder.serverId).isEqualTo("folder1")
        assertThat(folder.name).isEqualTo("New")
        assertThat(folder.type).isEqualTo("trash")
    }

    @Test
    fun `update folder settings`() {
        val folderId = sqliteDatabase.createFolder(
            inTopGroup = false,
            integrate = false,
            displayClass = "NO_CLASS",
            syncClass = "NO_CLASS",
            notifyClass = "NO_CLASS",
            pushClass = "NO_CLASS",
        )

        updateFolderOperations.updateFolderSettings(
            FolderDetails(
                folder = Folder(
                    id = folderId,
                    name = "irrelevant",
                    type = FolderType.REGULAR,
                    isLocalOnly = false,
                ),
                isInTopGroup = true,
                isIntegrate = true,
                displayClass = FolderClass.FIRST_CLASS,
                syncClass = FolderClass.FIRST_CLASS,
                notifyClass = FolderClass.FIRST_CLASS,
                pushClass = FolderClass.FIRST_CLASS,
            ),
        )

        val folder = sqliteDatabase.readFolders().first()
        assertThat(folder.id).isEqualTo(folderId)
        assertThat(folder.inTopGroup).isEqualTo(1)
        assertThat(folder.integrate).isEqualTo(1)
        assertThat(folder.displayClass).isEqualTo("FIRST_CLASS")
        assertThat(folder.syncClass).isEqualTo("FIRST_CLASS")
        assertThat(folder.notifyClass).isEqualTo("FIRST_CLASS")
        assertThat(folder.pushClass).isEqualTo("FIRST_CLASS")
    }

    @Test
    fun `update integrate setting`() {
        val folderId = sqliteDatabase.createFolder(integrate = false)

        updateFolderOperations.setIncludeInUnifiedInbox(folderId = folderId, includeInUnifiedInbox = true)

        val folder = sqliteDatabase.readFolders().first()
        assertThat(folder.id).isEqualTo(folderId)
        assertThat(folder.integrate).isEqualTo(1)
    }

    @Test
    fun `update display class`() {
        val folderId = sqliteDatabase.createFolder(displayClass = "FIRST_CLASS")

        updateFolderOperations.setDisplayClass(folderId = folderId, folderClass = FolderClass.SECOND_CLASS)

        val folder = sqliteDatabase.readFolders().first()
        assertThat(folder.id).isEqualTo(folderId)
        assertThat(folder.displayClass).isEqualTo("SECOND_CLASS")
    }

    @Test
    fun `update sync class`() {
        val folderId = sqliteDatabase.createFolder(syncClass = "FIRST_CLASS")

        updateFolderOperations.setSyncClass(folderId = folderId, folderClass = FolderClass.NO_CLASS)

        val folder = sqliteDatabase.readFolders().first()
        assertThat(folder.id).isEqualTo(folderId)
        assertThat(folder.syncClass).isEqualTo("NO_CLASS")
    }

    @Test
    fun `update push class`() {
        val folderId = sqliteDatabase.createFolder(pushClass = "FIRST_CLASS")

        updateFolderOperations.setPushClass(folderId = folderId, folderClass = FolderClass.NO_CLASS)

        val folder = sqliteDatabase.readFolders().first()
        assertThat(folder.id).isEqualTo(folderId)
        assertThat(folder.pushClass).isEqualTo("NO_CLASS")
    }

    @Test
    fun `update notification class`() {
        val folderId = sqliteDatabase.createFolder(syncClass = "FIRST_CLASS")

        updateFolderOperations.setNotificationClass(folderId = folderId, folderClass = FolderClass.INHERITED)

        val folder = sqliteDatabase.readFolders().first()
        assertThat(folder.id).isEqualTo(folderId)
        assertThat(folder.notifyClass).isEqualTo("INHERITED")
    }

    @Test
    fun `update more messages state`() {
        val folderId = sqliteDatabase.createFolder(moreMessages = "unknown")

        updateFolderOperations.setMoreMessages(folderId = folderId, moreMessages = MoreMessages.TRUE)

        val folder = sqliteDatabase.readFolders().first()
        assertThat(folder.id).isEqualTo(folderId)
        assertThat(folder.moreMessages).isEqualTo("true")
    }

    @Test
    fun `update late updated state`() {
        val folderId = sqliteDatabase.createFolder(lastUpdated = 23)

        updateFolderOperations.setLastChecked(folderId = folderId, timestamp = 42)

        val folder = sqliteDatabase.readFolders().first()
        assertThat(folder.id).isEqualTo(folderId)
        assertThat(folder.lastUpdated).isEqualTo(42)
    }

    @Test
    fun `update folder status`() {
        val folderId = sqliteDatabase.createFolder(status = null)

        updateFolderOperations.setStatus(folderId = folderId, status = "Sync error")

        val folder = sqliteDatabase.readFolders().first()
        assertThat(folder.id).isEqualTo(folderId)
        assertThat(folder.status).isEqualTo("Sync error")
    }

    @Test
    fun `update visible limit`() {
        val folderId = sqliteDatabase.createFolder(visibleLimit = 10)

        updateFolderOperations.setVisibleLimit(folderId = folderId, visibleLimit = 25)

        val folder = sqliteDatabase.readFolders().first()
        assertThat(folder.id).isEqualTo(folderId)
        assertThat(folder.visibleLimit).isEqualTo(25)
    }
}
