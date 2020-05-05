package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mailstore.MigrationsHelper

internal object MigrationTo67 {
    @JvmStatic
    fun addTypeColumnToFoldersTable(db: SQLiteDatabase, migrationsHelper: MigrationsHelper) {
        db.execSQL("ALTER TABLE folders ADD type TEXT DEFAULT \"regular\"")

        val account = migrationsHelper.account
        setFolderType(db, account.legacyInboxFolder, "inbox")
        setFolderType(db, "K9MAIL_INTERNAL_OUTBOX", "outbox")
        setFolderType(db, account.importedTrashFolder, "trash")
        setFolderType(db, account.importedDraftsFolder, "drafts")
        setFolderType(db, account.importedSpamFolder, "spam")
        setFolderType(db, account.importedSentFolder, "sent")
        setFolderType(db, account.importedArchiveFolder, "archive")
    }

    private fun setFolderType(db: SQLiteDatabase, serverId: String?, type: String) {
        if (serverId != null) {
            db.execSQL("UPDATE folders SET type = ? WHERE server_id = ?", arrayOf(type, serverId))
        }
    }
}
