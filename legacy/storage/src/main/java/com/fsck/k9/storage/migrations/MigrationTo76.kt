package com.fsck.k9.storage.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import app.k9mail.core.android.common.database.map
import com.fsck.k9.mailstore.MigrationsHelper
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.logging.legacy.Log

/**
 * Clean up special local folders
 *
 * In the past local special folders were not always created. For example, when importing settings or when setting up
 * an account, but checking the server settings didn't succeed and the user decided to continue anyway.
 *
 * Clicking "Next" in the incoming server settings screen would check the server settings and, in the case of success,
 * create new special local folders even if they already existed. So it's also possible existing installations have
 * multiple special local folders of one type.
 *
 * Here, we clean up local special folders to have exactly one of each type. Messages in additional folders will be
 * moved to the folder we keep and then the other folders will be deleted. An exception are messages in old Outbox
 * folders. They will be deleted and not be moved to the new/current Outbox folder because this would cause potentially
 * very old messages to be sent. The right thing would be to move them to the Drafts folder. But this is much more
 * complicated. They'd have to be uploaded if the Drafts folder is not a local folder. It's also not clear what should
 * happen if there is no Drafts folder configured.
 */
internal class MigrationTo76(private val db: SQLiteDatabase, private val migrationsHelper: MigrationsHelper) {
    fun cleanUpSpecialLocalFolders() {
        val account = migrationsHelper.account

        Log.v("Cleaning up Outbox folder")
        val outboxFolderId =
            account.outboxFolderId ?: createFolder("Outbox", "K9MAIL_INTERNAL_OUTBOX", OUTBOX_FOLDER_TYPE)
        deleteOtherOutboxFolders(outboxFolderId)
        account.outboxFolderId = outboxFolderId

        if (account.isPop3()) {
            Log.v("Cleaning up Drafts folder")
            val draftsFolderId = account.draftsFolderId ?: createFolder("Drafts", "Drafts", DRAFTS_FOLDER_TYPE)
            moveMessages(DRAFTS_FOLDER_TYPE, draftsFolderId)
            account.draftsFolderId = draftsFolderId

            Log.v("Cleaning up Sent folder")
            val sentFolderId = account.sentFolderId ?: createFolder("Sent", "Sent", SENT_FOLDER_TYPE)
            moveMessages(SENT_FOLDER_TYPE, sentFolderId)
            account.sentFolderId = sentFolderId

            Log.v("Cleaning up Trash folder")
            val trashFolderId = account.trashFolderId ?: createFolder("Trash", "Trash", TRASH_FOLDER_TYPE)
            moveMessages(TRASH_FOLDER_TYPE, trashFolderId)
            account.trashFolderId = trashFolderId
        }

        migrationsHelper.saveAccount()
    }

    private fun createFolder(name: String, serverId: String, type: String): Long {
        Log.v("  Creating new local folder (name=$name, serverId=$serverId, type=$type)…")
        val values = ContentValues().apply {
            put("name", name)
            put("visible_limit", 25)
            put("integrate", 0)
            put("top_group", 0)
            put("poll_class", "NO_CLASS")
            put("push_class", "SECOND_CLASS")
            put("display_class", "NO_CLASS")
            put("server_id", serverId)
            put("local_only", 1)
            put("type", type)
        }

        val folderId = db.insert("folders", null, values)
        Log.v("    Created folder with ID $folderId")

        return folderId
    }

    private fun deleteOtherOutboxFolders(outboxFolderId: Long) {
        val otherFolderIds = getOtherFolders(OUTBOX_FOLDER_TYPE, outboxFolderId)
        for (folderId in otherFolderIds) {
            deleteFolder(folderId)
        }
    }

    private fun getOtherFolders(folderType: String, excludeFolderId: Long): List<Long> {
        return db.query(
            "folders",
            arrayOf("id"),
            "local_only = 1 AND type = ? AND id != ?",
            arrayOf(folderType, excludeFolderId.toString()),
            null,
            null,
            null,
        ).use { cursor ->
            cursor.map { cursor.getLong(0) }
        }
    }

    private fun moveMessages(folderType: String, destinationFolderId: Long) {
        val sourceFolderIds = getOtherFolders(folderType, destinationFolderId)
        for (sourceFolderId in sourceFolderIds) {
            moveMessages(sourceFolderId, destinationFolderId)
            deleteFolder(sourceFolderId)
        }
    }

    private fun moveMessages(sourceFolderId: Long, destinationFolderId: Long) {
        Log.v("  Moving messages from folder [$sourceFolderId] to folder [$destinationFolderId]…")

        val values = ContentValues().apply {
            put("folder_id", destinationFolderId)
        }
        val rows = db.update("messages", values, "folder_id = ?", arrayOf(sourceFolderId.toString()))

        Log.v("    $rows messages moved.")
    }

    private fun deleteFolder(folderId: Long) {
        Log.v("  Deleting folder [$folderId]")
        db.delete("folders", "id = ?", arrayOf(folderId.toString()))
    }

    private fun LegacyAccount.isPop3() = incomingServerSettings.type == Protocols.POP3

    companion object {
        private const val OUTBOX_FOLDER_TYPE = "outbox"
        private const val DRAFTS_FOLDER_TYPE = "drafts"
        private const val SENT_FOLDER_TYPE = "sent"
        private const val TRASH_FOLDER_TYPE = "trash"
    }
}
