package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.fsck.k9.mailstore.MigrationsHelper
import com.fsck.k9.storage.messages.createFolder
import com.fsck.k9.storage.messages.createMessage
import com.fsck.k9.storage.messages.readFolders
import com.fsck.k9.storage.messages.readMessages
import kotlin.test.Test
import net.thunderbird.core.android.account.LegacyAccountDto
import org.junit.After
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTo91Test {
    private val database = createDatabaseVersion88()
    private val account = createAccount()
    private val migrationHelper = createMigrationsHelper(account)
    private val migration = MigrationTo91(database, migrationHelper)

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `should add account_id column to folders and messages tables`() {
        // Arrange
        val folderId = database.createFolder(name = "Folder")
        database.createMessage(folderId = folderId, subject = "Message")

        // Act
        migration.addAccountIdColumn()

        // Assert
        val folders = database.readFolders()
        assertThat(folders).isNotNull()
        assertThat(folders.size).isEqualTo(1)
        assertThat(folders[0].accountId).isEqualTo(ACCOUNT_UUID)

        val messages = database.readMessages()
        assertThat(messages).isNotNull()
        assertThat(messages.size).isEqualTo(1)
        assertThat(messages[0].accountId).isEqualTo(ACCOUNT_UUID)
    }

    @Test
    fun `should not fail if account_id column already exists`() {
        // Arrange
        migration.addAccountIdColumn()

        // Act
        migration.addAccountIdColumn()

        // Assert
        val folders = database.readFolders()
        assertThat(folders).isNotNull()
        assertThat(folders.size).isEqualTo(0)

        val messages = database.readMessages()
        assertThat(messages).isNotNull()
        assertThat(messages.size).isEqualTo(0)
    }

    private fun createAccount(): LegacyAccountDto {
        return mock {
            on { uuid } doReturn ACCOUNT_UUID
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

    @Suppress("LongMethod")
    private fun createDatabaseVersion88(): SQLiteDatabase {
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
                    notifications_enabled INTEGER DEFAULT 0,
                    more_messages TEXT default "unknown",
                    server_id TEXT,
                    local_only INTEGER,
                    type TEXT DEFAULT "regular",
                    visible INTEGER DEFAULT 1
                )
                """.trimIndent(),
            )

            execSQL(
                """
                CREATE TABLE messages (
                    id INTEGER PRIMARY KEY,
                    deleted INTEGER default 0,
                    folder_id INTEGER,
                    uid TEXT,
                    subject TEXT,
                    date INTEGER,
                    flags TEXT,
                    sender_list TEXT,
                    to_list TEXT,
                    cc_list TEXT,
                    bcc_list TEXT,
                    reply_to_list TEXT,
                    attachment_count INTEGER,
                    internal_date INTEGER,
                    message_id TEXT,
                    preview_type TEXT default "none",
                    preview TEXT,
                    mime_type TEXT,
                    normalized_subject_hash INTEGER,
                    empty INTEGER default 0,
                    read INTEGER default 0,
                    flagged INTEGER default 0,
                    answered INTEGER default 0,
                    forwarded INTEGER default 0,
                    message_part_id INTEGER,
                    encryption_type TEXT,
                    new_message INTEGER default 0
                )
                """.trimIndent(),
            )
        }
    }

    companion object {
        private const val ACCOUNT_UUID = "00000000-0000-0000-0000-000000000000"
    }
}
