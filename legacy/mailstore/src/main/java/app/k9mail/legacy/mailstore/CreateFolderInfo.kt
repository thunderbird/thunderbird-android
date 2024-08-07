package app.k9mail.legacy.mailstore

import com.fsck.k9.mail.FolderType

data class CreateFolderInfo(
    val serverId: String,
    val name: String,
    val type: FolderType,
    val settings: FolderSettings,
)
