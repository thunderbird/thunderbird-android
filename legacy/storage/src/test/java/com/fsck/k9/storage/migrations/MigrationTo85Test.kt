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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTo85Test {
    private var folderNotifyMode = FolderMode.ALL

    private val database = createDatabaseVersion84()
    private val account = createAccount()
    private val migrationHelper = createMigrationsHelper(account)
    private val migration = MigrationTo85(database, migrationHelper)

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `notification folders = FolderMode_NONE`() {
        folderNotifyMode = FolderMode.NONE
        database.createFolderVersion84(name = "One", notifyClass = "FIRST_CLASS")
        database.createFolderVersion84(name = "Two", notifyClass = "SECOND_CLASS")
        database.createFolderVersion84(name = "Three", notifyClass = "NO_CLASS")
        database.createFolderVersion84(name = "Four", notifyClass = "INHERITED", displayClass = "NO_CLASS")

        migration.addFoldersNotificationsEnabledColumn()

        assertThat(database.readFolders().toNotificationMapping()).containsExactlyInAnyOrder(
            "One" to false,
            "Two" to false,
            "Three" to false,
            "Four" to false,
        )
    }

    @Test
    fun `notification folders = FolderMode_ALL`() {
        folderNotifyMode = FolderMode.ALL
        database.createFolderVersion84(name = "One", notifyClass = "FIRST_CLASS")
        database.createFolderVersion84(name = "Two", notifyClass = "SECOND_CLASS")
        database.createFolderVersion84(name = "Three", notifyClass = "NO_CLASS")
        database.createFolderVersion84(name = "Four", notifyClass = "INHERITED", displayClass = "NO_CLASS")

        migration.addFoldersNotificationsEnabledColumn()

        assertThat(database.readFolders().toNotificationMapping()).containsExactlyInAnyOrder(
            "One" to true,
            "Two" to true,
            "Three" to true,
            "Four" to true,
        )
    }

    @Test
    fun `notification folders = FolderMode_FIRST_CLASS`() {
        folderNotifyMode = FolderMode.FIRST_CLASS
        database.createFolderVersion84(name = "One", notifyClass = "FIRST_CLASS")
        database.createFolderVersion84(name = "Two", notifyClass = "SECOND_CLASS")
        database.createFolderVersion84(name = "Three", notifyClass = "NO_CLASS")
        database.createFolderVersion84(name = "Four", notifyClass = "INHERITED", displayClass = "NO_CLASS")
        database.createFolderVersion84(name = "Five", notifyClass = "INHERITED", displayClass = "FIRST_CLASS")

        migration.addFoldersNotificationsEnabledColumn()

        assertThat(database.readFolders().toNotificationMapping()).containsExactlyInAnyOrder(
            "One" to true,
            "Two" to false,
            "Three" to false,
            "Four" to false,
            "Five" to true,
        )
    }

    @Test
    fun `notification folders = FolderMode_FIRST_AND_SECOND_CLASS`() {
        folderNotifyMode = FolderMode.FIRST_AND_SECOND_CLASS
        database.createFolderVersion84(name = "One", notifyClass = "FIRST_CLASS")
        database.createFolderVersion84(name = "Two", notifyClass = "SECOND_CLASS")
        database.createFolderVersion84(name = "Three", notifyClass = "NO_CLASS")
        database.createFolderVersion84(name = "Four", notifyClass = "INHERITED", displayClass = "NO_CLASS")
        database.createFolderVersion84(name = "Five", notifyClass = "INHERITED", displayClass = "FIRST_CLASS")
        database.createFolderVersion84(name = "Six", notifyClass = "INHERITED", displayClass = "SECOND_CLASS")

        migration.addFoldersNotificationsEnabledColumn()

        assertThat(database.readFolders().toNotificationMapping()).containsExactlyInAnyOrder(
            "One" to true,
            "Two" to true,
            "Three" to false,
            "Four" to false,
            "Five" to true,
            "Six" to true,
        )
    }

    @Test
    fun `notification folders = FolderMode_NOT_SECOND_CLASS`() {
        folderNotifyMode = FolderMode.NOT_SECOND_CLASS
        database.createFolderVersion84(name = "One", notifyClass = "FIRST_CLASS")
        database.createFolderVersion84(name = "Two", notifyClass = "SECOND_CLASS")
        database.createFolderVersion84(name = "Three", notifyClass = "NO_CLASS")
        database.createFolderVersion84(name = "Four", notifyClass = "INHERITED", displayClass = "NO_CLASS")
        database.createFolderVersion84(name = "Five", notifyClass = "INHERITED", displayClass = "FIRST_CLASS")
        database.createFolderVersion84(name = "Six", notifyClass = "INHERITED", displayClass = "SECOND_CLASS")

        migration.addFoldersNotificationsEnabledColumn()

        assertThat(database.readFolders().toNotificationMapping()).containsExactlyInAnyOrder(
            "One" to true,
            "Two" to false,
            "Three" to true,
            "Four" to true,
            "Five" to true,
            "Six" to false,
        )
    }

    @Test
    fun `notifications for special folders should be disabled`() {
        folderNotifyMode = FolderMode.ALL
        val myInboxFolderId = database.createFolderVersion84(name = "INBOX", notifyClass = "FIRST_CLASS")
        val myTrashFolderId = database.createFolderVersion84(name = "Trash", notifyClass = "FIRST_CLASS")
        val myDraftsFolderId = database.createFolderVersion84(name = "Drafts", notifyClass = "SECOND_CLASS")
        val mySpamFolderId = database.createFolderVersion84(name = "Spam", notifyClass = "NO_CLASS")
        val mySentFolderId = database.createFolderVersion84(name = "Sent", notifyClass = "INHERITED")
        database.createFolderVersion84(name = "Other", notifyClass = "NO_CLASS")
        account.stub {
            on { inboxFolderId } doReturn myInboxFolderId
            on { trashFolderId } doReturn myTrashFolderId
            on { draftsFolderId } doReturn myDraftsFolderId
            on { spamFolderId } doReturn mySpamFolderId
            on { sentFolderId } doReturn mySentFolderId
        }

        migration.addFoldersNotificationsEnabledColumn()

        assertThat(database.readFolders().toNotificationMapping()).containsExactlyInAnyOrder(
            "INBOX" to true,
            "Trash" to false,
            "Drafts" to false,
            "Spam" to false,
            "Sent" to false,
            "Other" to true,
        )
    }

    @Test
    fun `notifications for special folders that point to the inbox should follow inbox notification class`() {
        folderNotifyMode = FolderMode.FIRST_CLASS
        val myInboxFolderId = database.createFolderVersion84(name = "INBOX", notifyClass = "FIRST_CLASS")
        val myTrashFolderId = database.createFolderVersion84(name = "Trash", notifyClass = "FIRST_CLASS")
        val mySpamFolderId = database.createFolderVersion84(name = "Spam", notifyClass = "FIRST_CLASS")
        database.createFolderVersion84(name = "Other", notifyClass = "NO_CLASS")
        account.stub {
            on { inboxFolderId } doReturn myInboxFolderId
            on { trashFolderId } doReturn myTrashFolderId
            on { draftsFolderId } doReturn myInboxFolderId
            on { spamFolderId } doReturn mySpamFolderId
            on { sentFolderId } doReturn myInboxFolderId
        }

        migration.addFoldersNotificationsEnabledColumn()

        assertThat(database.readFolders().toNotificationMapping()).containsExactlyInAnyOrder(
            "INBOX" to true,
            "Trash" to false,
            "Spam" to false,
            "Other" to false,
        )
    }

    private fun createAccount(): LegacyAccountDto {
        return mock<LegacyAccountDto> {
            on { folderNotifyNewMailMode } doAnswer { folderNotifyMode }
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

    private fun createDatabaseVersion84(): SQLiteDatabase {
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
                    notify_class TEXT default 'INHERITED',
                    more_messages TEXT default "unknown",
                    server_id TEXT,
                    local_only INTEGER,
                    type TEXT DEFAULT "regular"
                )
                """.trimIndent(),
            )
        }
    }

    private fun SQLiteDatabase.createFolderVersion84(
        name: String = "irrelevant",
        type: String = "regular",
        serverId: String? = null,
        isLocalOnly: Boolean = true,
        integrate: Boolean = false,
        inTopGroup: Boolean = false,
        displayClass: String = "NO_CLASS",
        syncClass: String? = "INHERITED",
        notifyClass: String? = "INHERITED",
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
            put("notify_class", notifyClass)
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

    private fun List<FolderEntry>.toNotificationMapping(): List<Pair<String, Boolean>> {
        return map { folder ->
            folder.name!! to (folder.notificationsEnabled == 1)
        }
    }
}
