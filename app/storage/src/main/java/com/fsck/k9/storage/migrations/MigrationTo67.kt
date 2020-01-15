package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mailstore.MigrationsHelper

internal object MigrationTo67 {
    @JvmStatic
    fun addTypeColumnToFoldersTable(db: SQLiteDatabase, migrationsHelper: MigrationsHelper) {
        db.execSQL("ALTER TABLE folders ADD type TEXT DEFAULT \"regular\"")

        val account = migrationsHelper.account
        setFolderType(db, account.inboxFolder, "inbox")
        setFolderType(db, account.outboxFolder, "outbox")
        setFolderType(db, account.trashFolder, "trash")
        setFolderType(db, account.draftsFolder, "drafts")
        setFolderType(db, account.spamFolder, "spam")
        setFolderType(db, account.sentFolder, "sent")
        setFolderType(db, account.archiveFolder, "archive")
    }

    private fun setFolderType(db: SQLiteDatabase, serverId: String?, type: String) {
        if (serverId != null) {
            db.execSQL("UPDATE folders SET type = ? WHERE server_id = ?", arrayOf(type, serverId))
        }
    }
}
