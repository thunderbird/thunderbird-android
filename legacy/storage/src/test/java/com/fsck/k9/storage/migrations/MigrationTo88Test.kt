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
import net.thunderbird.core.android.account.LegacyAccount
import org.junit.After
import org.junit.runner.RunWith
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTo88Test {
    private var folderDisplayMode = FolderMode.ALL

    private val database = createDatabaseVersion87()
    private val account = createAccount()
    private val migrationHelper = createMigrationsHelper(account)
    private val migration = MigrationTo88(database, migrationHelper)

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `display folders = FolderMode_NONE`() {
        folderDisplayMode = FolderMode.NONE
        database.createFolderVersion86(name = "One", displayClass = "FIRST_CLASS")
        database.createFolderVersion86(name = "Two", displayClass = "SECOND_CLASS")
        database.createFolderVersion86(name = "Three", displayClass = "NO_CLASS")
        database.createFolderVersion86(name = "Four", displayClass = "INHERITED")

        migration.addFoldersVisibleColumn()

        assertThat(database.readFolders().toVisibleMapping()).containsExactlyInAnyOrder(
            "One" to false,
            "Two" to false,
            "Three" to false,
            "Four" to false,
        )
    }

    @Test
    fun `display folders = FolderMode_ALL`() {
        folderDisplayMode = FolderMode.ALL
        database.createFolderVersion86(name = "One", displayClass = "FIRST_CLASS")
        database.createFolderVersion86(name = "Two", displayClass = "SECOND_CLASS")
        database.createFolderVersion86(name = "Three", displayClass = "NO_CLASS")
        database.createFolderVersion86(name = "Four", displayClass = "INHERITED")

        migration.addFoldersVisibleColumn()

        assertThat(database.readFolders().toVisibleMapping()).containsExactlyInAnyOrder(
            "One" to true,
            "Two" to true,
            "Three" to true,
            "Four" to true,
        )
    }

    @Test
    fun `display folders = FolderMode_FIRST_CLASS`() {
        folderDisplayMode = FolderMode.FIRST_CLASS
        database.createFolderVersion86(name = "One", displayClass = "FIRST_CLASS")
        database.createFolderVersion86(name = "Two", displayClass = "SECOND_CLASS")
        database.createFolderVersion86(name = "Three", displayClass = "NO_CLASS")
        database.createFolderVersion86(name = "Four", displayClass = "INHERITED")

        migration.addFoldersVisibleColumn()

        assertThat(database.readFolders().toVisibleMapping()).containsExactlyInAnyOrder(
            "One" to true,
            "Two" to false,
            "Three" to false,
            "Four" to false,
        )
    }

    @Test
    fun `display folders = FolderMode_FIRST_AND_SECOND_CLASS`() {
        folderDisplayMode = FolderMode.FIRST_AND_SECOND_CLASS
        database.createFolderVersion86(name = "One", displayClass = "FIRST_CLASS")
        database.createFolderVersion86(name = "Two", displayClass = "SECOND_CLASS")
        database.createFolderVersion86(name = "Three", displayClass = "NO_CLASS")
        database.createFolderVersion86(name = "Four", displayClass = "INHERITED")

        migration.addFoldersVisibleColumn()

        assertThat(database.readFolders().toVisibleMapping()).containsExactlyInAnyOrder(
            "One" to true,
            "Two" to true,
            "Three" to false,
            "Four" to false,
        )
    }

    @Test
    fun `display folders = FolderMode_NOT_SECOND_CLASS`() {
        folderDisplayMode = FolderMode.NOT_SECOND_CLASS
        database.createFolderVersion86(name = "One", displayClass = "FIRST_CLASS")
        database.createFolderVersion86(name = "Two", displayClass = "SECOND_CLASS")
        database.createFolderVersion86(name = "Three", displayClass = "NO_CLASS")
        database.createFolderVersion86(name = "Four", displayClass = "INHERITED")

        migration.addFoldersVisibleColumn()

        assertThat(database.readFolders().toVisibleMapping()).containsExactlyInAnyOrder(
            "One" to true,
            "Two" to false,
            "Three" to true,
            "Four" to true,
        )
    }

    private fun createAccount(): LegacyAccount {
        return mock<LegacyAccount> {
            on { folderDisplayMode } doAnswer { folderDisplayMode }
        }
    }

    private fun createMigrationsHelper(account: LegacyAccount): MigrationsHelper {
        return object : MigrationsHelper {
            override fun getAccount(): LegacyAccount {
                return account
            }

            override fun saveAccount() {
                throw UnsupportedOperationException("not implemented")
            }
        }
    }

    private fun createDatabaseVersion87(): SQLiteDatabase {
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
                    sync_enabled INTEGER DEFAULT 0,
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
        syncEnabled: Boolean = false,
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
            put("sync_enabled", syncEnabled)
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

    private fun List<FolderEntry>.toVisibleMapping(): List<Pair<String, Boolean>> {
        return map { folder ->
            folder.name!! to (folder.visible == 1)
        }
    }
}
