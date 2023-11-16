package com.fsck.k9.mail.folders

import com.fsck.k9.mail.FolderType

data class RemoteFolder(
    val serverId: FolderServerId,
    val displayName: String,
    val type: FolderType,
)
