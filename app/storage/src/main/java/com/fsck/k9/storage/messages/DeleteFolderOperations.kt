package com.fsck.k9.storage.messages

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.K9
import com.fsck.k9.mailstore.LockableDatabase
import com.fsck.k9.mailstore.StorageManager
import java.io.File
import timber.log.Timber

internal class DeleteFolderOperations(
    private val storageManager: StorageManager,
    private val lockableDatabase: LockableDatabase,
    private val accountUuid: String
) {
    fun deleteFolders(folderServerIds: List<String>) {
        lockableDatabase.execute(true) { db ->
            for (folderServerId in folderServerIds) {
                db.deleteMessagePartFiles(folderServerId)
                db.deleteFolder(folderServerId)
            }
        }
    }

    private fun SQLiteDatabase.deleteMessagePartFiles(folderServerId: String) {
        rawQuery(
            """
            SELECT message_parts.id 
            FROM folders 
            JOIN messages ON (messages.folder_id = folders.id) 
            JOIN message_parts ON (
                message_parts.root = messages.message_part_id 
                AND 
                message_parts.data_location = $DATA_LOCATION_ON_DISK
            ) 
            WHERE folders.server_id = ?
            """.trimIndent(),
            arrayOf(folderServerId)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val messagePartId = cursor.getString(0)
                val file = getAttachmentFile(messagePartId)
                if (file.exists() && !file.delete() && K9.isDebugLoggingEnabled) {
                    Timber.w("Couldn't delete message part file: %s", file.absolutePath)
                }
            }
        }
    }

    private fun SQLiteDatabase.deleteFolder(folderServerId: String) {
        delete("folders", "server_id = ?", arrayOf(folderServerId))
    }

    private fun getAttachmentFile(messagePartId: String): File {
        val attachmentDirectory = storageManager.getAttachmentDirectory(accountUuid, lockableDatabase.storageProviderId)
        return File(attachmentDirectory, messagePartId)
    }
}
