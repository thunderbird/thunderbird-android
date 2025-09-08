package com.fsck.k9.storage.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import com.fsck.k9.mailstore.MigrationsHelper
import com.fsck.k9.storage.messages.FolderEntry
import com.fsck.k9.storage.messages.readFolders
import kotlin.test.Test
import net.thunderbird.core.android.account.FolderMode
import net.thunderbird.core.android.account.LegacyAccountDto
import org.junit.After
import org.junit.runner.RunWith
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTo87Test {
    private var folderSyncMode = FolderMode.ALL

    private val database = createDatabaseVersion86()
    private val account = createAccount()
    private val migrationHelper = createMigrationsHelper(account)
    private val migration = MigrationTo87(database, migrationHelper)

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `sync folders = FolderMode_NONE`() {
        folderSyncMode = FolderMode.NONE
        database.createFolderVersion86(name = "One", syncClass = "FIRST_CLASS")
        database.createFolderVersion86(name = "Two", syncClass = "SECOND_CLASS")
        database.createFolderVersion86(name = "Three", syncClass = "NO_CLASS")
        database.createFolderVersion86(name = "Four", syncClass = "INHERITED", displayClass = "NO_CLASS")

        migration.addFoldersSyncEnabledColumn()

        assertThat(database.readFolders().toSyncMapping()).containsExactlyInAnyOrder(
            "One" to false,
            "Two" to false,
            "Three" to false,
            "Four" to false,
        )
    }

    @Test
    fun `sync folders = FolderMode_ALL`() {
        folderSyncMode = FolderMode.ALL
        database.createFolderVersion86(name = "One", syncClass = "FIRST_CLASS")
        database.createFolderVersion86(name = "Two", syncClass = "SECOND_CLASS")
        database.createFolderVersion86(name = "Three", syncClass = "NO_CLASS")
        database.createFolderVersion86(name = "Four", syncClass = "INHERITED", displayClass = "NO_CLASS")

        migration.addFoldersSyncEnabledColumn()

        assertThat(database.readFolders().toSyncMapping()).containsExactlyInAnyOrder(
            "One" to true,
            "Two" to true,
            "Three" to true,
            "Four" to true,
        )
    }

    @Test
    fun `sync folders = FolderMode_FIRST_CLASS`() {
        folderSyncMode = FolderMode.FIRST_CLASS
        database.createFolderVersion86(name = "One", syncClass = "FIRST_CLASS")
        database.createFolderVersion86(name = "Two", syncClass = "SECOND_CLASS")
        database.createFolderVersion86(name = "Three", syncClass = "NO_CLASS")
        database.createFolderVersion86(name = "Four", syncClass = "INHERITED", displayClass = "NO_CLASS")
        database.createFolderVersion86(name = "Five", syncClass = "INHERITED", displayClass = "FIRST_CLASS")

        migration.addFoldersSyncEnabledColumn()

        assertThat(database.readFolders().toSyncMapping()).containsExactlyInAnyOrder(
            "One" to true,
            "Two" to false,
            "Three" to false,
            "Four" to false,
            "Five" to true,
        )
    }

    @Test
    fun `sync folders = FolderMode_FIRST_AND_SECOND_CLASS`() {
        folderSyncMode = FolderMode.FIRST_AND_SECOND_CLASS
        database.createFolderVersion86(name = "One", syncClass = "FIRST_CLASS")
        database.createFolderVersion86(name = "Two", syncClass = "SECOND_CLASS")
        database.createFolderVersion86(name = "Three", syncClass = "NO_CLASS")
        database.createFolderVersion86(name = "Four", syncClass = "INHERITED", displayClass = "NO_CLASS")
        database.createFolderVersion86(name = "Five", syncClass = "INHERITED", displayClass = "FIRST_CLASS")
        database.createFolderVersion86(name = "Six", syncClass = "INHERITED", displayClass = "SECOND_CLASS")

        migration.addFoldersSyncEnabledColumn()

        assertThat(database.readFolders().toSyncMapping()).containsExactlyInAnyOrder(
            "One" to true,
            "Two" to true,
            "Three" to false,
            "Four" to false,
            "Five" to true,
            "Six" to true,
        )
    }

    @Test
    fun `sync folders = FolderMode_NOT_SECOND_CLASS`() {
        folderSyncMode = FolderMode.NOT_SECOND_CLASS
        database.createFolderVersion86(name = "One", syncClass = "FIRST_CLASS")
        database.createFolderVersion86(name = "Two", syncClass = "SECOND_CLASS")
        database.createFolderVersion86(name = "Three", syncClass = "NO_CLASS")
        database.createFolderVersion86(name = "Four", syncClass = "INHERITED", displayClass = "NO_CLASS")
        database.createFolderVersion86(name = "Five", syncClass = "INHERITED", displayClass = "FIRST_CLASS")
        database.createFolderVersion86(name = "Six", syncClass = "INHERITED", displayClass = "SECOND_CLASS")

        migration.addFoldersSyncEnabledColumn()

        assertThat(database.readFolders().toSyncMapping()).containsExactlyInAnyOrder(
            "One" to true,
            "Two" to false,
            "Three" to true,
            "Four" to true,
            "Five" to true,
            "Six" to false,
        )
    }

    private fun createAccount(): LegacyAccountDto {
        return mock<LegacyAccountDto> {
            on { folderSyncMode } doAnswer { folderSyncMode }
        }
    }

    private fun createMigrationsHelper(account: LegacyAccountDto): MigrationsHelper {
        return object : MigrationsHelper {
            override fun getAccount(): LegacyAccountDto {
                return account
            }

            override fun saveAccount() {
                throw UnsupportedOperationException("not implemented")
            }
        }
    }

    private fun createDatabaseVersion86(): SQLiteDatabase {
        return SQLiteDatabase.create(null).apply {
            execSQL(
                """
                CREATE TABLE folders (
                    id INTEGER PRIMARY KEY,
                    name TEXT,
                    last_updated INTEGER,
                    unread_count INTEGER,
                    visible_limit INTEGER,
                    status TEXT,
                    flagged_count INTEGER default 0,
                    integrate INTEGER,
                    top_group INTEGER,
                    poll_class TEXT,
                    push_enabled INTEGER DEFAULT 0,
                    display_class TEXT,
                    notifications_enabled INTEGER DEFAULT 0,
                    more_messages TEXT default "unknown",
                    server_id TEXT,
                    local_only INTEGER,
                    type TEXT DEFAULT "regular"
                )
                """.trimIndent(),
            )
        }
    }

    private fun SQLiteDatabase.createFolderVersion86(
        name: String = "irrelevant",
        type: String = "regular",
        serverId: String? = null,
        isLocalOnly: Boolean = true,
        integrate: Boolean = false,
        inTopGroup: Boolean = false,
        displayClass: String = "NO_CLASS",
        syncClass: String? = "INHERITED",
        notificationsEnabled: Boolean = false,
        pushEnabled: Boolean = false,
        lastUpdated: Long = 0L,
        unreadCount: Int = 0,
        visibleLimit: Int = 25,
        status: String? = null,
        flaggedCount: Int = 0,
        moreMessages: String = "unknown",
    ): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("type", type)
            put("server_id", serverId)
            put("local_only", isLocalOnly)
            put("integrate", integrate)
            put("top_group", inTopGroup)
            put("display_class", displayClass)
            put("poll_class", syncClass)
            put("notifications_enabled", notificationsEnabled)
            put("push_enabled", pushEnabled)
            put("last_updated", lastUpdated)
            put("unread_count", unreadCount)
            put("visible_limit", visibleLimit)
            put("status", status)
            put("flagged_count", flaggedCount)
            put("more_messages", moreMessages)
        }

        return insert("folders", null, values)
    }

    private fun List<FolderEntry>.toSyncMapping(): List<Pair<String, Boolean>> {
        return map { folder ->
            folder.name!! to (folder.syncEnabled == 1)
        }
    }
}
