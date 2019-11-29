package com.fsck.k9.ui.folders

import android.content.res.Resources
import com.fsck.k9.mailstore.Folder
import com.fsck.k9.mailstore.FolderType
import com.fsck.k9.ui.R

class FolderNameFormatter(private val resources: Resources) {
    fun displayName(folder: Folder): String = when (folder.type) {
        FolderType.INBOX -> resources.getString(R.string.special_mailbox_name_inbox)
        FolderType.OUTBOX -> resources.getString(R.string.special_mailbox_name_outbox)
        else -> folder.name
    }
}
