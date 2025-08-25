package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mailstore.MigrationsHelper
import net.thunderbird.core.common.mail.Protocols

internal object MigrationTo65 {
    @JvmStatic
    fun addLocalOnlyColumnToFoldersTable(db: SQLiteDatabase, migrationsHelper: MigrationsHelper) {
        db.execSQL("ALTER TABLE folders ADD local_only INTEGER")

        if (isPop3Account(migrationsHelper)) {
            db.execSQL("UPDATE folders SET local_only = CASE server_id WHEN 'INBOX' THEN 0 ELSE 1 END")
        } else {
            db.execSQL("UPDATE folders SET local_only = CASE server_id WHEN 'K9MAIL_INTERNAL_OUTBOX' THEN 1 ELSE 0 END")
        }
    }

    private fun isPop3Account(migrationsHelper: MigrationsHelper): Boolean {
        return migrationsHelper.account.incomingServerSettings.type == Protocols.POP3
    }
}
