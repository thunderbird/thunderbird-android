package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.core.content.contentValuesOf
import app.k9mail.legacy.account.FolderMode
import com.fsck.k9.mailstore.MigrationsHelper

internal class MigrationTo88(private val db: SQLiteDatabase, private val migrationsHelper: MigrationsHelper) {
    fun addFoldersVisibleColumn() {
        addColumn()
        populateColumn()

        removeDisplayClassColumn()
    }

    private fun addColumn() {
        db.execSQL("ALTER TABLE folders ADD visible INTEGER DEFAULT 1")
    }

    private fun populateColumn() {
        val account = migrationsHelper.account

        // The default is for folders to be visible. So we only update folders that should be hidden.
        val whereClause = when (account.folderDisplayMode) {
            FolderMode.NONE -> {
                ""
            }
            FolderMode.ALL -> {
                return
            }
            FolderMode.FIRST_CLASS -> {
                "display_class != 'FIRST_CLASS'"
            }
            FolderMode.FIRST_AND_SECOND_CLASS -> {
                "display_class NOT IN ('FIRST_CLASS', 'SECOND_CLASS')"
            }
            FolderMode.NOT_SECOND_CLASS -> {
                "display_class = 'SECOND_CLASS'"
            }
        }

        db.update("folders", contentValuesOf("visible" to false), whereClause, null)
    }

    private fun removeDisplayClassColumn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            dropDisplayClassColumn()
        } else {
            clearDisplayClassColumn()
        }
    }

    private fun dropDisplayClassColumn() {
        db.execSQL("ALTER TABLE folders DROP COLUMN display_class")
    }

    private fun clearDisplayClassColumn() {
        db.update("folders", contentValuesOf("display_class" to null), null, null)
    }
}
