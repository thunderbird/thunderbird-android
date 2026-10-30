package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mailstore.MigrationsHelper

internal class MigrationTo75(private val db: SQLiteDatabase, private val migrationsHelper: MigrationsHelper) {
    fun updateAccountWithSpecialFolderIds() {
        // Look up the folder IDs first, so no database query runs while the account is being updated
        val account = migrationsHelper.account
        val inboxFolderId = getFolderId(account.legacyInboxFolder)
        val draftsFolderId = getFolderId(account.importedDraftsFolder)
        val sentFolderId = getFolderId(account.importedSentFolder)
        val trashFolderId = getFolderId(account.importedTrashFolder)
        val archiveFolderId = getFolderId(account.importedArchiveFolder)
        val spamFolderId = getFolderId(account.importedSpamFolder)
        val autoExpandFolderId = getFolderId(account.importedAutoExpandFolder)

        migrationsHelper.updateAccount { latestAccount ->
            latestAccount.copy(
                inboxFolderId = inboxFolderId,
                draftsFolderId = draftsFolderId,
                sentFolderId = sentFolderId,
                trashFolderId = trashFolderId,
                archiveFolderId = archiveFolderId,
                spamFolderId = spamFolderId,
                autoExpandFolderId = autoExpandFolderId,

                importedDraftsFolder = null,
                importedSentFolder = null,
                importedTrashFolder = null,
                importedArchiveFolder = null,
                importedSpamFolder = null,
                importedAutoExpandFolder = null,
            )
        }
    }

    private fun getFolderId(serverId: String?): Long? {
        if (serverId == null) return null

        return db.query("folders", arrayOf("id"), "server_id = ?", arrayOf(serverId), null, null, null).use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        }
    }
}
