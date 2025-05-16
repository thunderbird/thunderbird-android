package net.thunderbird.feature.mail.folder.api

data class Folder(
    val id: Long,
    val name: String,
    val type: FolderType,
    val isLocalOnly: Boolean,
)
