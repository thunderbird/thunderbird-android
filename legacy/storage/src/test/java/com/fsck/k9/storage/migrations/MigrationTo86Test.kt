package com.fsck.k9.storage.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.account.Account.FolderMode
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import com.fsck.k9.mailstore.MigrationsHelper
import com.fsck.k9.storage.messages.FolderEntry
import com.fsck.k9.storage.messages.readFolders
import kotlin.test.Test
import org.junit.After
import org.junit.runner.RunWith
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTo86Test {
    private var folderPushMode = FolderMode.ALL

    private val database = createDatabaseVersion85()
    private val account = createAccount()
    private val migrationHelper = createMigrationsHelper(account)
    private val migration = MigrationTo86(database, migrationHelper)

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `push folders = FolderMode_NONE`() {
        folderPushMode = FolderMode.NONE
        database.createFolderVersion85(name = "One", pushClass = "FIRST_CLASS")
        database.createFolderVersion85(name = "Two", pushClass = "SECOND_CLASS")
        database.createFolderVersion85(name = "Three", pushClass = "NO_CLASS")
        database.createFolderVersion85(name = "Four", pushClass = "INHERITED", displayClass = "NO_CLASS")

        migration.addFoldersPushEnabledColumn()

        assertThat(database.readFolders().toPushMapping()).containsExactlyInAnyOrder(
            "One" to false,
            "Two" to false,
            "Three" to false,
            "Four" to false,
        )
    }

    @Test
    fun `push folders = FolderMode_ALL`() {
        folderPushMode = FolderMode.ALL
        database.createFolderVersion85(name = "One", pushClass = "FIRST_CLASS")
        database.createFolderVersion85(name = "Two", pushClass = "SECOND_CLASS")
        database.createFolderVersion85(name = "Three", pushClass = "NO_CLASS")
        database.createFolderVersion85(name = "Four", pushClass = "INHERITED", displayClass = "NO_CLASS")

        migration.addFoldersPushEnabledColumn()

        assertThat(database.readFolders().toPushMapping()).containsExactlyInAnyOrder(
            "One" to true,
            "Two" to true,
            "Three" to true,
            "Four" to true,
        )
    }

    @Test
    fun `push folders = FolderMode_FIRST_CLASS`() {
        folderPushMode = FolderMode.FIRST_CLASS
        database.createFolderVersion85(name = "One", pushClass = "FIRST_CLASS")
        database.createFolderVersion85(name = "Two", pushClass = "SECOND_CLASS")
        database.createFolderVersion85(name = "Three", pushClass = "NO_CLASS")
        database.createFolderVersion85(name = "Four", pushClass = "INHERITED", displayClass = "NO_CLASS")
        database.createFolderVersion85(name = "Five", pushClass = "INHERITED", displayClass = "FIRST_CLASS")

        migration.addFoldersPushEnabledColumn()

        assertThat(database.readFolders().toPushMapping()).containsExactlyInAnyOrder(
            "One" to true,
            "Two" to false,
            "Three" to false,
            "Four" to false,
            "Five" to true,
        )
    }

    @Test
    fun `push folders = FolderMode_FIRST_AND_SECOND_CLASS`() {
        folderPushMode = FolderMode.FIRST_AND_SECOND_CLASS
        database.createFolderVersion85(name = "One", pushClass = "FIRST_CLASS")
        database.createFolderVersion85(name = "Two", pushClass = "SECOND_CLASS")
        database.createFolderVersion85(name = "Three", pushClass = "NO_CLASS")
        database.createFolderVersion85(name = "Four", pushClass = "INHERITED", displayClass = "NO_CLASS")
        database.createFolderVersion85(name = "Five", pushClass = "INHERITED", displayClass = "FIRST_CLASS")
        database.createFolderVersion85(name = "Six", pushClass = "INHERITED", displayClass = "SECOND_CLASS")

        migration.addFoldersPushEnabledColumn()

        assertThat(database.readFolders().toPushMapping()).containsExactlyInAnyOrder(
            "One" to true,
            "Two" to true,
            "Three" to false,
            "Four" to false,
            "Five" to true,
            "Six" to true,
        )
    }

    @Test
    fun `push folders = FolderMode_NOT_SECOND_CLASS`() {
        folderPushMode = FolderMode.NOT_SECOND_CLASS
        database.createFolderVersion85(name = "One", pushClass = "FIRST_CLASS")
        database.createFolderVersion85(name = "Two", pushClass = "SECOND_CLASS")
        database.createFolderVersion85(name = "Three", pushClass = "NO_CLASS")
        database.createFolderVersion85(name = "Four", pushClass = "INHERITED", displayClass = "NO_CLASS")
        database.createFolderVersion85(name = "Five", pushClass = "INHERITED", displayClass = "FIRST_CLASS")
        database.createFolderVersion85(name = "Six", pushClass = "INHERITED", displayClass = "SECOND_CLASS")

        migration.addFoldersPushEnabledColumn()

        assertThat(database.readFolders().toPushMapping()).containsExactlyInAnyOrder(
            "One" to true,
            "Two" to false,
            "Three" to true,
            "Four" to true,
            "Five" to true,
            "Six" to false,
        )
    }

    private fun createAccount(): Account {
        return mock<Account> {
            on { folderPushMode } doAnswer { folderPushMode }
        }
    }

    private fun createMigrationsHelper(account: Account): MigrationsHelper {
        return object : MigrationsHelper {
            override fun getAccount(): Account {
                return account
            }

            override fun saveAccount() {
                throw UnsupportedOperationException("not implemented")
            }
        }
    }

    private fun createDatabaseVersion85(): SQLiteDatabase {
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
                    push_class TEXT,
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

    private fun SQLiteDatabase.createFolderVersion85(
        name: String = "irrelevant",
        type: String = "regular",
        serverId: String? = null,
        isLocalOnly: Boolean = true,
        integrate: Boolean = false,
        inTopGroup: Boolean = false,
        displayClass: String = "NO_CLASS",
        syncClass: String? = "INHERITED",
        notificationsEnabled: Boolean = false,
        pushClass: String? = "INHERITED",
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
            put("push_class", pushClass)
            put("last_updated", lastUpdated)
            put("unread_count", unreadCount)
            put("visible_limit", visibleLimit)
            put("status", status)
            put("flagged_count", flaggedCount)
            put("more_messages", moreMessages)
        }

        return insert("folders", null, values)
    }

    private fun List<FolderEntry>.toPushMapping(): List<Pair<String, Boolean>> {
        return map { folder ->
            folder.name!! to (folder.pushEnabled == 1)
        }
    }
}
