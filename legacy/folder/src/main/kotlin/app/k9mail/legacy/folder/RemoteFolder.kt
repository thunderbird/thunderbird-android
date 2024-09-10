package app.k9mail.legacy.folder

import app.k9mail.core.mail.folder.api.FolderType

data class RemoteFolder(
    val id: Long,
    val serverId: String,
    val name: String,
    val type: FolderType,
)
