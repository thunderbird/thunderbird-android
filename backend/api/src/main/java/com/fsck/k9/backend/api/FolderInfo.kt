package com.fsck.k9.backend.api

import com.fsck.k9.mail.FolderType
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter

data class FolderInfo(
    val serverId: String,
    val name: String,
    val type: FolderType,
    val folderPathDelimiter: FolderPathDelimiter? = null,
)
