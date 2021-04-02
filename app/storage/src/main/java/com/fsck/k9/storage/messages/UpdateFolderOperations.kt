package com.fsck.k9.storage.messages

import android.content.ContentValues
import com.fsck.k9.mailstore.FolderDetails
import com.fsck.k9.mailstore.LockableDatabase

internal class UpdateFolderOperations(private val lockableDatabase: LockableDatabase) {
    fun updateFolderSettings(folderDetails: FolderDetails) {
        lockableDatabase.execute(false) { db ->
            val contentValues = ContentValues().apply {
                put("top_group", folderDetails.isInTopGroup)
                put("integrate", folderDetails.isIntegrate)
                put("poll_class", folderDetails.syncClass.name)
                put("display_class", folderDetails.displayClass.name)
                put("notify_class", folderDetails.notifyClass.name)
                put("push_class", folderDetails.pushClass.name)
            }

            db.update("folders", contentValues, "id = ?", arrayOf(folderDetails.folder.id.toString()))
        }
    }
}
