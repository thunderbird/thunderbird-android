package com.fsck.k9.storage.messages

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fsck.k9.storage.RobolectricTest
import org.junit.Test

class CheckFolderOperationsTest : RobolectricTest() {
    private val sqliteDatabase = createDatabase()
    private val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
    private val checkFolderOperations = CheckFolderOperations(lockableDatabase)

    @Test
    fun `single folder not included in Unified Inbox`() {
        val folderIds = listOf(sqliteDatabase.createFolder(integrate = false))

        val result = checkFolderOperations.areAllIncludedInUnifiedInbox(folderIds)

        assertThat(result).isFalse()
    }

    @Test
    fun `single folder included in Unified Inbox`() {
        val folderIds = listOf(sqliteDatabase.createFolder(integrate = true))

        val result = checkFolderOperations.areAllIncludedInUnifiedInbox(folderIds)

        assertThat(result).isTrue()
    }

    @Test
    fun `not all folders included in Unified Inbox`() {
        val folderIds = listOf(
            sqliteDatabase.createFolder(integrate = true),
            sqliteDatabase.createFolder(integrate = false),
        )

        val result = checkFolderOperations.areAllIncludedInUnifiedInbox(folderIds)

        assertThat(result).isFalse()
    }

    @Test
    fun `1000 folders included in Unified Inbox`() {
        val folderIds = List(1000) {
            sqliteDatabase.createFolder(integrate = true)
        }

        val result = checkFolderOperations.areAllIncludedInUnifiedInbox(folderIds)

        assertThat(result).isTrue()
    }

    @Test
    fun `999 of 1000 folders included in Unified Inbox`() {
        val folderIds = List(999) {
            sqliteDatabase.createFolder(integrate = true)
        } + sqliteDatabase.createFolder(integrate = false)

        val result = checkFolderOperations.areAllIncludedInUnifiedInbox(folderIds)

        assertThat(result).isFalse()
    }
}
