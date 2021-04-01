package com.fsck.k9.storage.messages

import com.fsck.k9.mail.FolderClass
import com.fsck.k9.mail.FolderType
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
    fun `get non-existent folder should return null`() {
        val result = retrieveFolderOperations.getFolder(1) { "failed" }

        assertThat(result).isNull()
    }
}
