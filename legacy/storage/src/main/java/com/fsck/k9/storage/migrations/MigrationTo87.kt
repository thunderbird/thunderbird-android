package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.core.content.contentValuesOf
import com.fsck.k9.mailstore.MigrationsHelper
import net.thunderbird.core.android.account.FolderMode

internal class MigrationTo87(private val db: SQLiteDatabase, private val migrationsHelper: MigrationsHelper) {
    fun addFoldersSyncEnabledColumn() {
        addColumn()
        populateColumn()

        removePollClassColumn()
    }

    private fun addColumn() {
        db.execSQL("ALTER TABLE folders ADD sync_enabled INTEGER DEFAULT 0")
    }

    @Suppress("LongMethod")
    private fun populateColumn() {
        val account = migrationsHelper.account

        val whereClause = when (account.folderSyncMode) {
            FolderMode.NONE -> {
                return
            }
            FolderMode.ALL -> {
                ""
            }
            FolderMode.FIRST_CLASS -> {
                """
                poll_class = 'FIRST_CLASS' OR (
                  poll_class = 'INHERITED' AND display_class = 'FIRST_CLASS'
                )
                """.trimIndent()
            }
            FolderMode.FIRST_AND_SECOND_CLASS -> {
                """
                poll_class IN ('FIRST_CLASS', 'SECOND_CLASS') OR (
                  poll_class = 'INHERITED' AND display_class IN ('FIRST_CLASS', 'SECOND_CLASS')
                )
                """.trimIndent()
            }
            FolderMode.NOT_SECOND_CLASS -> {
                """
                poll_class IN ('NO_CLASS', 'FIRST_CLASS') OR (
                  poll_class = 'INHERITED' AND display_class IN ('NO_CLASS', 'FIRST_CLASS')
                )
                """.trimIndent()
            }
        }

        db.update("folders", contentValuesOf("sync_enabled" to true), whereClause, null)
    }

    private fun removePollClassColumn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            dropPollClassColumn()
        } else {
            clearPollClassColumn()
        }
    }

    private fun dropPollClassColumn() {
        db.execSQL("ALTER TABLE folders DROP COLUMN poll_class")
    }

    private fun clearPollClassColumn() {
        db.update("folders", contentValuesOf("poll_class" to null), null, null)
    }
}
