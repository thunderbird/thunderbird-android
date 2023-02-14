package com.fsck.k9.mailstore

import com.fsck.k9.mail.FolderType

data class CreateFolderInfo(
    val serverId: String,
    val name: String,
    val type: FolderType,
    val settings: FolderSettings,
)
