package com.fsck.k9.storage.messages

import android.database.sqlite.SQLiteDatabase
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fsck.k9.storage.RobolectricTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class CheckFolderOperationsTest : RobolectricTest() {
    private lateinit var sqliteDatabase: SQLiteDatabase
    private lateinit var checkFolderOperations: CheckFolderOperations

    @Before
    fun setUp() {
        sqliteDatabase = createDatabase()
        val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
        checkFolderOperations = CheckFolderOperations(lockableDatabase)
    }

    @After
    fun tearDown() {
        sqliteDatabase.close()
    }

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

    @Test
    fun `hasPushEnabledFolder() with one folder enabled for push`() {
        sqliteDatabase.createFolder(pushEnabled = true)
        sqliteDatabase.createFolder(pushEnabled = false)

        val result = checkFolderOperations.hasPushEnabledFolder()

        assertThat(result).isTrue()
    }

    @Test
    fun `hasPushEnabledFolder() with no folder enabled for push`() {
        sqliteDatabase.createFolder(pushEnabled = false)
        sqliteDatabase.createFolder(pushEnabled = false)

        val result = checkFolderOperations.hasPushEnabledFolder()

        assertThat(result).isFalse()
    }
}
