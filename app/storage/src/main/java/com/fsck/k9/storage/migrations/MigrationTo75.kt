package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mailstore.MigrationsHelper

internal class MigrationTo75(private val db: SQLiteDatabase, private val migrationsHelper: MigrationsHelper) {
    fun updateAccountWithSpecialFolderIds() {
        val account = migrationsHelper.account

        setSpecialFolderId(account.legacyInboxFolder, account::setInboxFolderId)
        setSpecialFolderId("K9MAIL_INTERNAL_OUTBOX", account::setOutboxFolderId)
        setSpecialFolderId(account.draftsFolder, account::setDraftsFolderId)
        setSpecialFolderId(account.sentFolder, account::setSentFolderId)
        setSpecialFolderId(account.trashFolder, account::setTrashFolderId)
        setSpecialFolderId(account.archiveFolder, account::setArchiveFolderId)
        setSpecialFolderId(account.spamFolder, account::setSpamFolderId)
        setSpecialFolderId(account.autoExpandFolder, account::setAutoExpandFolderId)

        account.draftsFolder = null
        account.sentFolder = null
        account.trashFolder = null
        account.archiveFolder = null
        account.spamFolder = null
        account.autoExpandFolder = null

        migrationsHelper.saveAccount()
    }

    private fun setSpecialFolderId(serverId: String?, setFolderId: (Long) -> Unit) {
        if (serverId == null) return

        db.query("folders", arrayOf("id"), "server_id = ?", arrayOf(serverId), null, null, null).use { cursor ->
            if (cursor.moveToFirst()) {
                val folderId = cursor.getLong(0)
                setFolderId(folderId)
            }
        }
    }
}
