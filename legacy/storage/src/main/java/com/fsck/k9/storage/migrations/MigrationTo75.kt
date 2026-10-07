package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mailstore.MigrationsHelper

internal class MigrationTo75(private val db: SQLiteDatabase, private val migrationsHelper: MigrationsHelper) {
    fun updateAccountWithSpecialFolderIds() {
        val account = migrationsHelper.account
        val updatedAccount = account.copy(
            inboxFolderId = getFolderId(account.legacyInboxFolder),
            draftsFolderId = getFolderId(account.importedDraftsFolder),
            sentFolderId = getFolderId(account.importedSentFolder),
            trashFolderId = getFolderId(account.importedTrashFolder),
            archiveFolderId = getFolderId(account.importedArchiveFolder),
            spamFolderId = getFolderId(account.importedSpamFolder),
            autoExpandFolderId = getFolderId(account.importedAutoExpandFolder),

            importedDraftsFolder = null,
            importedSentFolder = null,
            importedTrashFolder = null,
            importedArchiveFolder = null,
            importedSpamFolder = null,
            importedAutoExpandFolder = null,
        )

        migrationsHelper.saveAccount(updatedAccount)
    }

    private fun getFolderId(serverId: String?): Long? {
        if (serverId == null) return null

        return db.query("folders", arrayOf("id"), "server_id = ?", arrayOf(serverId), null, null, null).use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        }
    }
}
