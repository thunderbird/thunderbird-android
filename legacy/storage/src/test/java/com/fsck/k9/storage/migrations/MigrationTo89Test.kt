package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTo89Test {
    private val database = createDatabaseVersion88()

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `should add size column to messages table`() {
        MigrationTo89(database).addMessageSizeColumn()

        assertColumnExists(database, "messages", "size")
    }

    private fun createDatabaseVersion88(): SQLiteDatabase {
        return SQLiteDatabase.create(null).apply {
            execSQL(
                "CREATE TABLE messages (" +
                    "id INTEGER PRIMARY KEY, " +
                    "deleted INTEGER default 0, " +
                    "folder_id INTEGER, " +
                    "uid TEXT, " +
                    "subject TEXT, " +
                    "date INTEGER, " +
                    "flags TEXT, " +
                    "sender_list TEXT, " +
                    "to_list TEXT, " +
                    "cc_list TEXT, " +
                    "bcc_list TEXT, " +
                    "reply_to_list TEXT, " +
                    "attachment_count INTEGER, " +
                    "internal_date INTEGER, " +
                    "message_id TEXT, " +
                    "preview_type TEXT default \"none\", " +
                    "preview TEXT, " +
                    "mime_type TEXT, " +
                    "normalized_subject_hash INTEGER, " +
                    "empty INTEGER default 0, " +
                    "read INTEGER default 0, " +
                    "flagged INTEGER default 0, " +
                    "answered INTEGER default 0, " +
                    "forwarded INTEGER default 0, " +
                    "message_part_id INTEGER," +
                    "encryption_type TEXT," +
                    "new_message INTEGER DEFAULT 0" +
                    ")",
            )
        }
    }

    private fun assertColumnExists(db: SQLiteDatabase, tableName: String, columnName: String) {
        val cursor = db.rawQuery("PRAGMA table_info($tableName)", null)
        cursor.use {
            val columnIndex = cursor.getColumnIndex("name")
            assertTrue("Column index should be valid", columnIndex >= 0)

            var columnExists = false
            while (cursor.moveToNext()) {
                val currentColumnName = cursor.getString(columnIndex)
                if (currentColumnName == columnName) {
                    columnExists = true
                    break
                }
            }
            assertTrue("Column '$columnName' should exist in table '$tableName'", columnExists)
        }
    }
}
