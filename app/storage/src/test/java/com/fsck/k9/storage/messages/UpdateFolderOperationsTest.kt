package com.fsck.k9.storage.messages

import com.fsck.k9.mail.FolderClass
import com.fsck.k9.mailstore.Folder
import com.fsck.k9.mailstore.FolderDetails
import com.fsck.k9.mailstore.FolderType
import com.fsck.k9.storage.RobolectricTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UpdateFolderOperationsTest : RobolectricTest() {
    private val sqliteDatabase = createDatabase()
    private val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
    private val updateFolderOperations = UpdateFolderOperations(lockableDatabase)

    @Test
    fun `update folder settings`() {
        val folderId = sqliteDatabase.createFolder(
            inTopGroup = false,
            integrate = false,
            displayClass = "NO_CLASS",
            syncClass = "NO_CLASS",
            notifyClass = "NO_CLASS",
            pushClass = "NO_CLASS"
        )

        updateFolderOperations.updateFolderSettings(
            FolderDetails(
                folder = Folder(
                    id = folderId,
                    name = "irrelevant",
                    type = FolderType.REGULAR,
                    isLocalOnly = false
                ),
                isInTopGroup = true,
                isIntegrate = true,
                displayClass = FolderClass.FIRST_CLASS,
                syncClass = FolderClass.FIRST_CLASS,
                notifyClass = FolderClass.FIRST_CLASS,
                pushClass = FolderClass.FIRST_CLASS
            )
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
}
