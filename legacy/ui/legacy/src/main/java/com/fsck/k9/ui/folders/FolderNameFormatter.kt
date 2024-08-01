package com.fsck.k9.ui.folders

import android.content.res.Resources
import app.k9mail.legacy.folder.Folder
import app.k9mail.legacy.folder.FolderType
import com.fsck.k9.mailstore.RemoteFolder
import com.fsck.k9.ui.R

class FolderNameFormatter(private val resources: Resources) {
    fun displayName(folder: Folder): String {
        return if (folder.isLocalOnly) {
            localFolderDisplayName(folder)
        } else {
            remoteFolderDisplayName(folder)
        }
    }

    private fun localFolderDisplayName(folder: Folder) = when (folder.type) {
        FolderType.OUTBOX -> resources.getString(R.string.special_mailbox_name_outbox)
        FolderType.DRAFTS -> resources.getString(R.string.special_mailbox_name_drafts)
        FolderType.SENT -> resources.getString(R.string.special_mailbox_name_sent)
        FolderType.TRASH -> resources.getString(R.string.special_mailbox_name_trash)
        else -> folder.name
    }

    private fun remoteFolderDisplayName(folder: Folder) = when (folder.type) {
        FolderType.INBOX -> resources.getString(R.string.special_mailbox_name_inbox)
        else -> folder.name
    }

    fun displayName(folder: RemoteFolder) = when (folder.type) {
        FolderType.INBOX -> resources.getString(R.string.special_mailbox_name_inbox)
        else -> folder.name
    }
}
