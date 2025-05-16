package net.thunderbird.feature.mail.folder.api

data class RemoteFolder(
    val id: Long,
    val serverId: String,
    val name: String,
    val type: FolderType,
)
