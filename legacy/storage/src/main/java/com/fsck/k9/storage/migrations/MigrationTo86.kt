package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.core.content.contentValuesOf
import app.k9mail.legacy.account.Account.FolderMode
import com.fsck.k9.mailstore.MigrationsHelper

internal class MigrationTo86(private val db: SQLiteDatabase, private val migrationsHelper: MigrationsHelper) {
    fun addFoldersPushEnabledColumn() {
        addColumn()
        populateColumn()

        removePushClassColumn()
    }

    private fun addColumn() {
        db.execSQL("ALTER TABLE folders ADD push_enabled INTEGER DEFAULT 0")
    }

    @Suppress("LongMethod")
    private fun populateColumn() {
        val account = migrationsHelper.account

        val whereClause = when (account.folderPushMode) {
            FolderMode.NONE -> {
                return
            }
            FolderMode.ALL -> {
                ""
            }
            FolderMode.FIRST_CLASS -> {
                """
                push_class = 'FIRST_CLASS' OR (
                  push_class = 'INHERITED' AND (
                    poll_class = 'FIRST_CLASS' OR (
                      poll_class = 'INHERITED' AND display_class = 'FIRST_CLASS'
                    )
                  )
                )
                """.trimIndent()
            }
            FolderMode.FIRST_AND_SECOND_CLASS -> {
                """
                push_class IN ('FIRST_CLASS', 'SECOND_CLASS') OR (
                  push_class = 'INHERITED' AND (
                    poll_class IN ('FIRST_CLASS', 'SECOND_CLASS') OR (
                      poll_class = 'INHERITED' AND display_class IN ('FIRST_CLASS', 'SECOND_CLASS')
                    )
                  )
                )
                """.trimIndent()
            }
            FolderMode.NOT_SECOND_CLASS -> {
                """
                push_class IN ('NO_CLASS', 'FIRST_CLASS') OR (
                  push_class = 'INHERITED' AND (
                    poll_class IN ('NO_CLASS', 'FIRST_CLASS') OR (
                      poll_class = 'INHERITED' AND display_class IN ('NO_CLASS', 'FIRST_CLASS')
                    )
                  )
                )
                """.trimIndent()
            }
        }

        db.update("folders", contentValuesOf("push_enabled" to true), whereClause, null)
    }

    private fun removePushClassColumn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            dropPushClassColumn()
        } else {
            clearPushClassColumn()
        }
    }

    private fun dropPushClassColumn() {
        db.execSQL("ALTER TABLE folders DROP COLUMN push_class")
    }

    private fun clearPushClassColumn() {
        db.update("folders", contentValuesOf("push_class" to null), null, null)
    }
}
