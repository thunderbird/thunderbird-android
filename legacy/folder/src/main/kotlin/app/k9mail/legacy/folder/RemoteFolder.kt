package app.k9mail.legacy.folder

data class RemoteFolder(
    val id: Long,
    val serverId: String,
    val name: String,
    val type: FolderType,
)
