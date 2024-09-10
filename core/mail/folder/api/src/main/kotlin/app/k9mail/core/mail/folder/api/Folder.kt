package app.k9mail.core.mail.folder.api

data class Folder(
    val id: Long,
    val name: String,
    val type: FolderType,
    val isLocalOnly: Boolean,
)
