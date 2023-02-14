package com.fsck.k9.storage.messages

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mailstore.LockableDatabase

internal class DeleteFolderOperations(
    private val lockableDatabase: LockableDatabase,
    private val attachmentFileManager: AttachmentFileManager,
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
            """,
            arrayOf(folderServerId),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val messagePartId = cursor.getLong(0)
                attachmentFileManager.deleteFile(messagePartId)
            }
        }
    }

    private fun SQLiteDatabase.deleteFolder(folderServerId: String) {
        delete("folders", "server_id = ?", arrayOf(folderServerId))
    }
}
