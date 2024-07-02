package com.fsck.k9.storage.messages

import android.content.ContentValues
import com.fsck.k9.mailstore.CreateFolderInfo
import com.fsck.k9.mailstore.LockableDatabase
import com.fsck.k9.mailstore.toDatabaseFolderType

internal class CreateFolderOperations(private val lockableDatabase: LockableDatabase) {
    fun createFolders(folders: List<CreateFolderInfo>) {
        lockableDatabase.execute(true) { db ->
            for (folder in folders) {
                val folderSettings = folder.settings
                val values = ContentValues().apply {
                    put("name", folder.name)
                    put("visible_limit", folderSettings.visibleLimit)
                    put("integrate", folderSettings.integrate)
                    put("top_group", folderSettings.inTopGroup)
                    put("poll_class", folderSettings.syncClass.name)
                    put("push_class", folderSettings.pushClass.name)
                    put("display_class", folderSettings.displayClass.name)
                    put("notify_class", folderSettings.notifyClass.name)
                    put("server_id", folder.serverId)
                    put("local_only", false)
                    put("type", folder.type.toDatabaseFolderType())
                }

                db.insert("folders", null, values)
            }
        }
    }
}
