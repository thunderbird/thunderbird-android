package net.thunderbird.feature.folder.api

import net.thunderbird.feature.mail.folder.api.FolderType

data class RemoteFolder(
    val id: Long,
    val serverId: String,
    val name: String,
    val type: FolderType,
)
