package com.fsck.k9.storage.messages

import app.k9mail.legacy.mailstore.MoreMessages
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.none
import assertk.assertions.prop
import com.fsck.k9.storage.RobolectricTest
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderDetails
import net.thunderbird.feature.mail.folder.api.FolderType
import org.junit.Before
import org.junit.Test
import com.fsck.k9.mail.FolderType as RemoteFolderType

class UpdateFolderOperationsTest : RobolectricTest() {
    private val sqliteDatabase = createDatabase()
    private val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
    private val updateFolderOperations = UpdateFolderOperations(lockableDatabase)

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

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
            visible = false,
            syncEnabled = false,
            notificationsEnabled = false,
            pushEnabled = false,
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
                isVisible = true,
                isSyncEnabled = true,
                isNotificationsEnabled = true,
                isPushEnabled = true,
            ),
        )

        val folder = sqliteDatabase.readFolders().first()
        assertThat(folder.id).isEqualTo(folderId)
        assertThat(folder.inTopGroup).isEqualTo(1)
        assertThat(folder.integrate).isEqualTo(1)
        assertThat(folder.visible).isEqualTo(1)
        assertThat(folder.syncEnabled).isEqualTo(1)
        assertThat(folder.notificationsEnabled).isEqualTo(1)
        assertThat(folder.pushEnabled).isEqualTo(1)
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
    fun `update visible setting`() {
        val folderId = sqliteDatabase.createFolder(visible = true)

        updateFolderOperations.setVisible(folderId = folderId, visible = false)

        val folder = sqliteDatabase.readFolders().first()
        assertThat(folder.id).isEqualTo(folderId)
        assertThat(folder.visible).isEqualTo(0)
    }

    @Test
    fun `update sync setting`() {
        val folderId = sqliteDatabase.createFolder(syncEnabled = true)

        updateFolderOperations.setSyncEnabled(folderId = folderId, enable = false)

        val folder = sqliteDatabase.readFolders().first()
        assertThat(folder.id).isEqualTo(folderId)
        assertThat(folder.syncEnabled).isEqualTo(0)
    }

    @Test
    fun `update push class`() {
        val folderId = sqliteDatabase.createFolder(pushEnabled = true)

        updateFolderOperations.setPushEnabled(folderId = folderId, enable = false)

        val folder = sqliteDatabase.readFolders().first()
        assertThat(folder.id).isEqualTo(folderId)
        assertThat(folder.pushEnabled).isEqualTo(0)
    }

    @Test
    fun `update notifications setting`() {
        val folderId = sqliteDatabase.createFolder(notificationsEnabled = false)

        updateFolderOperations.setNotificationsEnabled(folderId = folderId, enable = true)

        val folder = sqliteDatabase.readFolders().first()
        assertThat(folder.id).isEqualTo(folderId)
        assertThat(folder.notificationsEnabled).isEqualTo(1)
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

    @Test
    fun `disable push for all folders`() {
        sqliteDatabase.createFolder(pushEnabled = true)
        sqliteDatabase.createFolder(pushEnabled = false)

        updateFolderOperations.setPushDisabled()

        assertThat(sqliteDatabase.readFolders()).none {
            it.prop(FolderEntry::pushEnabled).isEqualTo(1)
        }
    }
}
