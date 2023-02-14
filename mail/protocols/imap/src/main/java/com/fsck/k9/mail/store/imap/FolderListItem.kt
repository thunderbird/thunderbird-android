package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.FolderType

data class FolderListItem(
    val serverId: String,
    val name: String,
    val type: FolderType,
    val oldServerId: String?,
)
