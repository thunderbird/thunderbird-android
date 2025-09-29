package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mailstore.MigrationsHelper

internal class MigrationTo75(private val db: SQLiteDatabase, private val migrationsHelper: MigrationsHelper) {
    fun updateAccountWithSpecialFolderIds() {
        val account = migrationsHelper.account

        account.inboxFolderId = getFolderId(account.legacyInboxFolder)
        account.draftsFolderId = getFolderId(account.importedDraftsFolder)
        account.sentFolderId = getFolderId(account.importedSentFolder)
        account.trashFolderId = getFolderId(account.importedTrashFolder)
        account.archiveFolderId = getFolderId(account.importedArchiveFolder)
        account.spamFolderId = getFolderId(account.importedSpamFolder)
        account.autoExpandFolderId = getFolderId(account.importedAutoExpandFolder)

        account.importedDraftsFolder = null
        account.importedSentFolder = null
        account.importedTrashFolder = null
        account.importedArchiveFolder = null
        account.importedSpamFolder = null
        account.importedAutoExpandFolder = null

        migrationsHelper.saveAccount()
    }

    private fun getFolderId(serverId: String?): Long? {
        if (serverId == null) return null

        return db.query("folders", arrayOf("id"), "server_id = ?", arrayOf(serverId), null, null, null).use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        }
    }
}
