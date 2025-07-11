package com.fsck.k9.storage.messages

import android.content.ContentValues
import com.fsck.k9.mailstore.LockableDatabase

internal class FolderNameSanitizer(private val lockableDatabase: LockableDatabase) {
    fun removeGmailPrefixFromFolders() {
        lockableDatabase.execute(false) { db ->
            val cursor = db.query(
                "folders",
                arrayOf("id", "name"),
                "name LIKE ? OR name LIKE ?",
                arrayOf("%[Gmail]/%", "%[Google Mail]/%"),
                null,
                null,
                null,
            )

            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val updatedName = name
                    .replace("[Gmail]/", "")
                    .replace("[Google Mail]/", "")

                val values = ContentValues().apply {
                    put("name", updatedName)
                }

                db.update("folders", values, "id = ?", arrayOf(id.toString()))
            }

            cursor.close()
        }
    }
}
