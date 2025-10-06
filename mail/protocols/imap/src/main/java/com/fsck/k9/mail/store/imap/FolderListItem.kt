package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.FolderType
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter

data class FolderListItem(
    val serverId: String,
    val name: String,
    val type: FolderType,
    val folderPathDelimiter: FolderPathDelimiter? = null,
)
