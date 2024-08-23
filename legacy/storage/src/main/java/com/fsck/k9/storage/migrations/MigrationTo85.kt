package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.core.content.contentValuesOf
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.account.Account.FolderMode
import com.fsck.k9.mailstore.MigrationsHelper

internal class MigrationTo85(private val db: SQLiteDatabase, private val migrationsHelper: MigrationsHelper) {
    fun addFoldersNotificationsEnabledColumn() {
        addColumn()
        populateColumn()

        removeNotifyClassColumn()
    }

    private fun addColumn() {
        db.execSQL("ALTER TABLE folders ADD notifications_enabled INTEGER DEFAULT 0")
    }

    @Suppress("LongMethod")
    private fun populateColumn() {
        val account = migrationsHelper.account
        val ignoreFolders = getNotificationIgnoredFolders(account)

        val ignoreFoldersWhereClause = if (ignoreFolders.isNotEmpty()) {
            "id NOT IN (${ignoreFolders.joinToString(separator = ",") { "?" }})"
        } else {
            "1"
        }

        val whereClause = when (account.folderNotifyNewMailMode) {
            FolderMode.NONE -> {
                return
            }
            FolderMode.ALL -> {
                ignoreFoldersWhereClause
            }
            FolderMode.FIRST_CLASS -> {
                """
                (
                  notify_class = 'FIRST_CLASS' OR (
                    notify_class = 'INHERITED' AND (
                      push_class = 'FIRST_CLASS' OR (
                        push_class = 'INHERITED' AND (
                          poll_class = 'FIRST_CLASS' OR (
                            poll_class = 'INHERITED' AND display_class = 'FIRST_CLASS'
                          )
                        )
                      )
                    )
                  )
                ) AND $ignoreFoldersWhereClause
                """.trimIndent()
            }
            FolderMode.FIRST_AND_SECOND_CLASS -> {
                """
                (
                  notify_class IN ('FIRST_CLASS', 'SECOND_CLASS') OR (
                    notify_class = 'INHERITED' AND (
                      push_class IN ('FIRST_CLASS', 'SECOND_CLASS') OR (
                        push_class = 'INHERITED' AND (
                          poll_class IN ('FIRST_CLASS', 'SECOND_CLASS') OR (
                            poll_class = 'INHERITED' AND display_class IN ('FIRST_CLASS', 'SECOND_CLASS')
                          )
                        )
                      )
                    )
                  )
                ) AND $ignoreFoldersWhereClause
                """.trimIndent()
            }
            FolderMode.NOT_SECOND_CLASS -> {
                """
                (
                  notify_class IN ('NO_CLASS', 'FIRST_CLASS') OR (
                    notify_class = 'INHERITED' AND (
                      push_class IN ('NO_CLASS', 'FIRST_CLASS') OR (
                        push_class = 'INHERITED' AND (
                          poll_class IN ('NO_CLASS', 'FIRST_CLASS') OR (
                            poll_class = 'INHERITED' AND display_class IN ('NO_CLASS', 'FIRST_CLASS')
                          )
                        )
                      )
                    )
                  )
                ) AND $ignoreFoldersWhereClause
                """.trimIndent()
            }
        }

        db.update("folders", contentValuesOf("notifications_enabled" to true), whereClause, ignoreFolders)
    }

    private fun getNotificationIgnoredFolders(account: Account): Array<String> {
        val inboxFolderId = account.inboxFolderId

        // These special folders were ignored via K9NotificationStrategy unless they were pointing to the inbox.
        return listOf(
            account.trashFolderId,
            account.draftsFolderId,
            account.spamFolderId,
            account.sentFolderId,
        ).asSequence()
            .filterNotNull()
            .filterNot { it == inboxFolderId }
            .map { it.toString() }
            .toList()
            .toTypedArray()
    }

    private fun removeNotifyClassColumn() {
        // Support for dropping columns was added in SQLite 3.35.0 (2021-03-12).
        // See https://www.sqlite.org/releaselog/3_35_5.html
        //
        // So a SQLite version containing support for dropping tables is only available starting with API 34.
        // See https://developer.android.com/reference/android/database/sqlite/package-summary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            dropNotifyClassColumn()
        } else {
            clearNotifyClassColumn()
        }
    }

    private fun dropNotifyClassColumn() {
        db.execSQL("ALTER TABLE folders DROP COLUMN notify_class")
    }

    private fun clearNotifyClassColumn() {
        db.update("folders", contentValuesOf("notify_class" to null), null, null)
    }
}
